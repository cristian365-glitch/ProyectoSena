package com.conexiones;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import untils.PasswordHasher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/GoogleCallbackServlet")
public class GoogleCallbackServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        System.out.println("🔵 Callback de Google recibido");
        
        // 1. Verificar que no haya errores
        String error = request.getParameter("error");
        if (error != null) {
            System.out.println("❌ Error de Google: " + error);
            response.sendRedirect(request.getContextPath() + "/login/Login.html?error=google_auth_error");
            return;
        }
        
        // 2. Obtener el código de autorización
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        
        if (code == null || code.isEmpty()) {
            System.out.println("❌ No se recibió código de autorización");
            response.sendRedirect(request.getContextPath() + "/login/Login.html?error=google_no_code");
            return;
        }
        
        // 3. Verificar el state token (anti-CSRF)
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("❌ No hay sesión activa");
            response.sendRedirect(request.getContextPath() + "/login/Login.html?error=session_expired");
            return;
        }
        
        String expectedState = (String) session.getAttribute("google_oauth_state");
        if (expectedState == null || !expectedState.equals(state)) {
            System.out.println("❌ State token inválido - posible ataque CSRF");
            response.sendRedirect(request.getContextPath() + "/login/Login.html?error=invalid_state");
            return;
        }
        
        // Limpiar el state usado
        session.removeAttribute("google_oauth_state");
        
        System.out.println("✅ State token verificado");
        System.out.println("🔵 Intercambiando código por token...");
        
        try {
            // 4. Intercambiar código por access token
            String accessToken = intercambiarCodigoPorToken(code);
            
            if (accessToken == null) {
                System.out.println("❌ No se pudo obtener access token");
                response.sendRedirect(request.getContextPath() + "/login/Login.html?error=google_token_error");
                return;
            }
            
            System.out.println("✅ Access token obtenido");
            System.out.println("🔵 Obteniendo información del usuario...");
            
            // 5. Obtener información del usuario de Google
            GoogleUserInfo userInfo = obtenerInfoUsuario(accessToken);
            
            if (userInfo == null) {
                System.out.println("❌ No se pudo obtener información del usuario");
                response.sendRedirect(request.getContextPath() + "/login/Login.html?error=google_userinfo_error");
                return;
            }
            
            System.out.println("✅ Información del usuario obtenida:");
            System.out.println("   Email: " + userInfo.email);
            System.out.println("   Nombre: " + userInfo.name);
            System.out.println("   Verificado: " + userInfo.emailVerified);
            
            // 6. Buscar o crear usuario en la BD
            boolean loginExitoso = procesarUsuarioGoogle(request, response, userInfo);
            
            if (loginExitoso) {
                System.out.println("✅ Login con Google exitoso");
                response.sendRedirect(request.getContextPath() + "/index.html?login=google");
            } else {
                System.out.println("❌ Error al procesar usuario");
                response.sendRedirect(request.getContextPath() + "/login/Login.html?error=google_process_error");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en callback de Google:");
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/login/Login.html?error=google_error");
        }
    }
    
    private String intercambiarCodigoPorToken(String code) throws IOException {
        URL url = new URL(GoogleOAuthConfig.TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        StringBuilder postData = new StringBuilder();
        postData.append("code=").append(URLEncoder.encode(code, "UTF-8"));
        postData.append("&client_id=").append(URLEncoder.encode(GoogleOAuthConfig.getClientId(), "UTF-8"));
        postData.append("&client_secret=").append(URLEncoder.encode(GoogleOAuthConfig.getClientSecret(), "UTF-8"));
        postData.append("&redirect_uri=").append(URLEncoder.encode(GoogleOAuthConfig.getRedirectUri(), "UTF-8"));
        postData.append("&grant_type=authorization_code");
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.err.println("❌ Error al obtener token. Código: " + responseCode);
            return null;
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
        return json.has("access_token") ? json.get("access_token").getAsString() : null;
    }
    
    private GoogleUserInfo obtenerInfoUsuario(String accessToken) throws IOException {
        URL url = new URL(GoogleOAuthConfig.USERINFO_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.err.println("❌ Error al obtener info de usuario. Código: " + responseCode);
            return null;
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
        
        GoogleUserInfo userInfo = new GoogleUserInfo();
        userInfo.email = json.has("email") ? json.get("email").getAsString() : null;
        userInfo.name = json.has("name") ? json.get("name").getAsString() : null;
        userInfo.picture = json.has("picture") ? json.get("picture").getAsString() : null;
        userInfo.emailVerified = json.has("email_verified") && json.get("email_verified").getAsBoolean();
        userInfo.googleId = json.has("sub") ? json.get("sub").getAsString() : null;
        
        return userInfo;
    }
    
    private boolean procesarUsuarioGoogle(HttpServletRequest request, HttpServletResponse response,
                                         GoogleUserInfo userInfo) throws IOException {
        
        if (userInfo.email == null || !userInfo.emailVerified) {
            System.out.println("❌ Email no verificado o no disponible");
            return false;
        }
        
        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psInsert = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            
            String checkSql = "SELECT id, nombre, telefono FROM usuarios WHERE email = ?";
            psCheck = conn.prepareStatement(checkSql);
            psCheck.setString(1, userInfo.email.toLowerCase());
            rs = psCheck.executeQuery();
            
            int userId;
            String nombre;
            String telefono;
            
            if (rs.next()) {
                userId = rs.getInt("id");
                nombre = rs.getString("nombre");
                telefono = rs.getString("telefono");
                
                System.out.println("✅ Usuario existente encontrado: " + userInfo.email);
                
                String updateSql = "UPDATE usuarios SET google_id = ?, foto_url = ? WHERE id = ? AND (google_id IS NULL OR foto_url IS NULL)";
                psUpdate = conn.prepareStatement(updateSql);
                psUpdate.setString(1, userInfo.googleId);
                psUpdate.setString(2, userInfo.picture);
                psUpdate.setInt(3, userId);
                psUpdate.executeUpdate();
                
            } else {
                System.out.println("🆕 Creando nuevo usuario desde Google: " + userInfo.email);
                
                byte[] salt = PasswordHasher.generarSalt();
                String saltBase64 = Base64.getEncoder().encodeToString(salt);
                String passwordHash = PasswordHasher.hashPassword(generarPasswordAleatorio(), salt);
                
                String insertSql = "INSERT INTO usuarios (nombre, email, telefono, password_hash, salt, google_id, foto_url) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?)";
                psInsert = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
                psInsert.setString(1, userInfo.name != null ? userInfo.name : "Usuario de Google");
                psInsert.setString(2, userInfo.email.toLowerCase());
                psInsert.setString(3, "");
                psInsert.setString(4, passwordHash);
                psInsert.setString(5, saltBase64);
                psInsert.setString(6, userInfo.googleId);
                psInsert.setString(7, userInfo.picture);
                
                int filas = psInsert.executeUpdate();
                
                if (filas == 0) {
                    conn.rollback();
                    return false;
                }
                
                ResultSet generatedKeys = psInsert.getGeneratedKeys();
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                    nombre = userInfo.name;
                    telefono = "";
                } else {
                    conn.rollback();
                    return false;
                }
            }
            
            conn.commit();
            
            HttpSession session = request.getSession(true);
            session.setAttribute("usuario", nombre);
            session.setAttribute("userId", userId);
            session.setAttribute("email", userInfo.email);
            session.setAttribute("telefono", telefono);
            session.setAttribute("foto_url", userInfo.picture);
            session.setAttribute("logueado", true);
            session.setAttribute("esAdmin", false);
            session.setAttribute("loginMethod", "google");
            session.setMaxInactiveInterval(30 * 60);
            
            request.changeSessionId();
            
            String clientIp = getClientIP(request);
            LogManager.registrarLog(userId, clientIp, false);
            
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            
            System.err.println("❌ Error SQL procesando usuario Google:");
            e.printStackTrace();
            return false;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            
            System.err.println("❌ Error procesando usuario Google:");
            e.printStackTrace();
            return false;
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (psCheck != null) try { psCheck.close(); } catch (SQLException e) {}
            if (psInsert != null) try { psInsert.close(); } catch (SQLException e) {}
            if (psUpdate != null) try { psUpdate.close(); } catch (SQLException e) {}
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
    
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
    
    private String generarPasswordAleatorio() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    private static class GoogleUserInfo {
        String email;
        String name;
        String picture;
        boolean emailVerified;
        String googleId;
    }
}