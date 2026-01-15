package com.conexiones;

import untils.PasswordHasher;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    
    // URL base de Gravatar para avatares
    private static final String GRAVATAR_URL = "https://www.gravatar.com/avatar/";
    
    // Protección contra fuerza bruta
    private static final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_TIME = TimeUnit.MINUTES.toMillis(15);
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
        
        if (!dbManager.testConnection()) {
            throw new ServletException("No se puede conectar a la base de datos");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        System.out.println("🔐 Intento de login: " + email);
        
        if (email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/Login.html?error=campos_vacios");
            return;
        }
        
        email = email.trim().toLowerCase();
        
        // Verificar protección fuerza bruta
        String clientIp = getClientIP(request);
        if (estaBloqueado(clientIp)) {
            System.out.println("🚫 IP bloqueada por demasiados intentos: " + clientIp);
            response.sendRedirect(request.getContextPath() + "/Login.html?error=demasiados_intentos");
            return;
        }
        
        // Primero intentar login como ADMIN
        if (intentarLoginAdmin(request, response, email, password, clientIp)) {
            limpiarIntentosLogin(clientIp);
            return;
        }
        
        // Si no es admin, intentar login como usuario normal
        if (intentarLoginUsuario(request, response, email, password, clientIp)) {
            limpiarIntentosLogin(clientIp);
            return;
        }
        
        // Login falló - registrar intento
        registrarIntentoFallido(clientIp);
        
        // Delay aleatorio para prevenir timing attacks
        try {
            Thread.sleep(1000 + new SecureRandom().nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("❌ Login fallido para: " + email);
        response.sendRedirect(request.getContextPath() + "/Login.html?error=credenciales_incorrectas");
    }
    
    /**
     * Intenta hacer login con credenciales de administrador
     */
    private boolean intentarLoginAdmin(HttpServletRequest request, HttpServletResponse response, 
                                       String email, String password, String clientIp) throws IOException {
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, nombre, password_hash, salt FROM usuarios_admin WHERE email = ? AND activo = 1")) {
            
            ps.setString(1, email);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    
                    System.out.println("🔍 Admin encontrado, verificando contraseña...");
                    
                    if (PasswordHasher.verificarPassword(password, storedHash, salt)) {
                        String nombreAdmin = rs.getString("nombre");
                        int adminId = rs.getInt("id");
                        
                        HttpSession session = request.getSession(true);
                        session.setAttribute("usuario", nombreAdmin);
                        session.setAttribute("userId", adminId);
                        session.setAttribute("email", email);
                        session.setAttribute("logueado", true);
                        session.setAttribute("esAdmin", true);
                        session.setAttribute("loginMethod", "password"); // ✅ NUEVO
                        session.setMaxInactiveInterval(30 * 60);
                        
                        request.changeSessionId();
                        
                        // ✅ NUEVO: Actualizar fecha de último login
                        actualizarUltimoLogin(adminId, true);
                        
                        LogManager.registrarLog(adminId, clientIp, true);
                        
                        System.out.println("✅ Login admin exitoso: " + email);
                        response.sendRedirect(request.getContextPath() + "/index.html?login=admin");
                        return true;
                    } else {
                        System.out.println("❌ Contraseña incorrecta para admin: " + email);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en login admin: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Intenta hacer login con credenciales de usuario normal
     */
    private boolean intentarLoginUsuario(HttpServletRequest request, HttpServletResponse response,
                                        String email, String password, String clientIp) throws IOException {
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, nombre, telefono, password_hash, salt FROM usuarios WHERE email = ?")) {
            
            ps.setString(1, email);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    
                    System.out.println("🔍 Usuario encontrado, verificando contraseña...");
                    
                    if (PasswordHasher.verificarPassword(password, storedHash, salt)) {
                        String nombreUsuario = rs.getString("nombre");
                        String telefono = rs.getString("telefono");
                        int userId = rs.getInt("id");
                        
                        HttpSession session = request.getSession(true);
                        session.setAttribute("usuario", nombreUsuario);
                        session.setAttribute("userId", userId);
                        session.setAttribute("email", email);
                        session.setAttribute("telefono", telefono);
                        session.setAttribute("logueado", true);
                        session.setAttribute("esAdmin", false);
                        session.setAttribute("loginMethod", "password"); // ✅ NUEVO
                        session.setMaxInactiveInterval(30 * 60);
                        
                        request.changeSessionId();
                        
                        // ✅ NUEVO: Actualizar fecha de último login
                        actualizarUltimoLogin(userId, false);
                        
                        LogManager.registrarLog(userId, clientIp, false);
                        
                        System.out.println("✅ Login usuario exitoso: " + email);
                        response.sendRedirect(request.getContextPath() + "/index.html?login=exitoso");
                        return true;
                    } else {
                        System.out.println("❌ Contraseña incorrecta para usuario: " + email);
                    }
                } else {
                    System.out.println("⚠️ Usuario no encontrado: " + email);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error en login: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/Login.html?error=error_sistema");
        }
        
        return false;
    }
    
    /**
     * ✅ NUEVO MÉTODO: Actualiza la fecha de último login del usuario
     */
    private void actualizarUltimoLogin(int userId, boolean esAdmin) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = dbManager.getConnection();
            
            String sql = esAdmin ? 
                "UPDATE usuarios_admin SET fecha_ultimo_login = NOW() WHERE id = ?" :
                "UPDATE usuarios SET fecha_ultimo_login = NOW() WHERE id = ?";
            
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
            
            System.out.println("✅ Fecha de último login actualizada para usuario ID: " + userId);
            
        } catch (SQLException e) {
            System.err.println("⚠️ Error actualizando último login: " + e.getMessage());
            // No es un error crítico, solo registramos el log
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
    
    /**
     * Obtiene la IP real del cliente
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * Verifica si una IP está bloqueada
     */
    private boolean estaBloqueado(String ip) {
        LoginAttempt attempt = loginAttempts.get(ip);
        if (attempt == null) {
            return false;
        }
        
        long tiempoTranscurrido = System.currentTimeMillis() - attempt.lastAttempt;
        
        if (tiempoTranscurrido > LOCKOUT_TIME) {
            loginAttempts.remove(ip);
            return false;
        }
        
        return attempt.attempts >= MAX_ATTEMPTS;
    }
    
    /**
     * Registra un intento fallido de login
     */
    private void registrarIntentoFallido(String ip) {
        loginAttempts.compute(ip, (k, v) -> {
            if (v == null) {
                return new LoginAttempt(1, System.currentTimeMillis());
            } else {
                return new LoginAttempt(v.attempts + 1, System.currentTimeMillis());
            }
        });
        
        LoginAttempt attempt = loginAttempts.get(ip);
        System.out.println("⚠️ Intento fallido desde IP: " + ip + " (intento " + attempt.attempts + "/" + MAX_ATTEMPTS + ")");
    }
    
    /**
     * Limpia los intentos de login de una IP
     */
    private void limpiarIntentosLogin(String ip) {
        loginAttempts.remove(ip);
    }
    
    // ============================================
    // MÉTODOS PARA GESTIÓN DE AVATARES
    // ============================================
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if ("cerrarSesion".equals(action)) {
            cerrarSesion(request, response);
            
        } else if ("getCsrfToken".equals(action)) {
            generarTokenCSRFResponse(request, response);
            
        } else if ("getAvatarUrl".equals(action)) {
            obtenerUrlAvatar(request, response);
            
        } else if ("getAvatar".equals(action)) {
            obtenerImagenAvatar(request, response);
            
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Acción no válida");
        }
    }
    
    /**
     * Devuelve la URL del avatar del usuario en formato JSON
     */
    private void obtenerUrlAvatar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        String email = null;
        
        if (session != null && session.getAttribute("email") != null) {
            email = (String) session.getAttribute("email");
        }
        
        String avatarUrl;
        if (email != null && !email.isEmpty()) {
            String emailHash = generarMD5(email.trim().toLowerCase());
            
            int size = 200;
            String sizeParam = request.getParameter("size");
            if (sizeParam != null) {
                try {
                    size = Integer.parseInt(sizeParam);
                    size = Math.min(2048, Math.max(1, size));
                } catch (NumberFormatException e) {
                    size = 200;
                }
            }
            
            avatarUrl = GRAVATAR_URL + emailHash + "?d=mp&s=" + size;
        } else {
            avatarUrl = GRAVATAR_URL + "00000000000000000000000000000000?d=mp&s=200";
        }
        
        response.getWriter().write("{\"avatarUrl\":\"" + avatarUrl + "\"}");
    }
    
    /**
     * Devuelve la imagen del avatar directamente como bytes
     */
    private void obtenerImagenAvatar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        HttpSession session = request.getSession(false);
        String email = null;
        
        if (session != null && session.getAttribute("email") != null) {
            email = (String) session.getAttribute("email");
        }
        
        if (email == null || email.isEmpty()) {
            response.sendRedirect(GRAVATAR_URL + "00000000000000000000000000000000?d=mp&s=200");
            return;
        }
        
        String emailHash = generarMD5(email.trim().toLowerCase());
        
        int size = 200;
        String sizeParam = request.getParameter("size");
        if (sizeParam != null) {
            try {
                size = Integer.parseInt(sizeParam);
                size = Math.min(2048, Math.max(1, size));
            } catch (NumberFormatException e) {
                size = 200;
            }
        }
        
        String avatarUrl = GRAVATAR_URL + emailHash + "?d=mp&s=" + size;
        
        try {
            URL url = new URL(avatarUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                response.setContentType(conn.getContentType());
                response.setContentLength(conn.getContentLength());
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                try (InputStream in = conn.getInputStream();
                     OutputStream out = response.getOutputStream()) {
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                response.sendRedirect(GRAVATAR_URL + "00000000000000000000000000000000?d=mp&s=" + size);
            }
            
        } catch (Exception e) {
            System.err.println("Error al obtener avatar: " + e.getMessage());
            response.sendRedirect(GRAVATAR_URL + "00000000000000000000000000000000?d=mp&s=" + size);
        }
    }
    
    /**
     * Genera hash MD5 de un string
     */
    private String generarMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar hash MD5", e);
        }
    }
    
    /**
     * Cierra la sesión del usuario
     */
    private void cerrarSesion(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String usuario = (String) session.getAttribute("usuario");
            System.out.println("👋 Cerrando sesión de: " + usuario);
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/index.html?logout=exitoso");
    }
    
    /**
     * Genera y devuelve un token CSRF
     */
    private void generarTokenCSRFResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(true);
        String token = generarTokenCSRF();
        session.setAttribute("csrf_token", token);
        
        response.setContentType("application/json");
        response.getWriter().write("{\"csrf_token\":\"" + token + "\"}");
    }
    
    /**
     * Genera un token CSRF aleatorio y seguro
     */
    private String generarTokenCSRF() {
        SecureRandom random = new SecureRandom();
        byte[] token = new byte[32];
        random.nextBytes(token);
        return Base64.getEncoder().encodeToString(token);
    }
    
    /**
     * Clase interna para tracking de intentos de login
     */
    private static class LoginAttempt {
        int attempts;
        long lastAttempt;
        
        LoginAttempt(int attempts, long lastAttempt) {
            this.attempts = attempts;
            this.lastAttempt = lastAttempt;
        }
    }
}