package com.conexiones;

import untils.PasswordHasher;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.security.spec.InvalidKeySpecException;

/**
 * Servlet para gestión completa del perfil de usuario
 * 
 * Funcionalidades:
 * - GET con action=obtenerPerfil: Obtiene datos completos del usuario
 * - GET con action=obtenerAvatar: Obtiene URL del avatar de Gravatar
 * - POST con action=actualizarDatos: Actualiza nombre, teléfono, etc.
 * - POST con action=cambiarPassword: Cambia la contraseña
 * - POST con action=actualizarEmail: Actualiza email (regenera avatar)
 */
@WebServlet("/PerfilServlet")
public class PerfilServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    private Gson gson = new Gson();
    
    private static final String GRAVATAR_URL = "https://www.gravatar.com/avatar/";
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
        
        if (!dbManager.testConnection()) {
            throw new ServletException("No se puede conectar a la base de datos");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("logueado") == null) {
            enviarError(response, "No hay sesión activa");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            if ("obtenerPerfil".equals(action)) {
                obtenerPerfilCompleto(request, response, session);
            } else if ("obtenerAvatar".equals(action)) {
                obtenerUrlAvatar(request, response, session);
            } else if ("verificarEmailDisponible".equals(action)) {
                verificarEmailDisponible(request, response, session);
            } else {
                enviarError(response, "Acción no válida");
            }
        } catch (Exception e) {
            System.err.println("❌ Error en PerfilServlet GET: " + e.getMessage());
            e.printStackTrace();
            enviarError(response, "Error del servidor: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("logueado") == null) {
            enviarError(response, "No hay sesión activa");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            if ("actualizarDatos".equals(action)) {
                actualizarDatosPersonales(request, response, session);
            } else if ("cambiarPassword".equals(action)) {
                cambiarPassword(request, response, session);
            } else if ("actualizarEmail".equals(action)) {
                actualizarEmail(request, response, session);
            } else {
                enviarError(response, "Acción no válida");
            }
        } catch (Exception e) {
            System.err.println("❌ Error en PerfilServlet POST: " + e.getMessage());
            e.printStackTrace();
            enviarError(response, "Error del servidor: " + e.getMessage());
        }
    }
    
    // ============================================
    // MÉTODOS GET - CONSULTA DE DATOS
    // ============================================
    
    /**
     * Obtiene el perfil completo del usuario actual
     * 
     * Devuelve JSON con:
     * {
     *   "success": true,
     *   "usuario": {
     *     "id": 123,
     *     "nombre": "Juan Pérez",
     *     "email": "juan@example.com",
     *     "telefono": "+57 300 1234567",
     *     "avatarUrl": "https://www.gravatar.com/avatar/...",
     *     "fechaRegistro": "2024-01-15"
     *   }
     * }
     */
    private void obtenerPerfilCompleto(HttpServletRequest request, HttpServletResponse response, 
                                       HttpSession session) throws IOException {
        
        Integer userId = (Integer) session.getAttribute("userId");
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        
        if (userId == null) {
            enviarError(response, "No se encontró ID de usuario en sesión");
            return;
        }
        
        // Determinar tabla según tipo de usuario
        String tabla = (esAdmin != null && esAdmin) ? "usuarios_admin" : "usuarios";
        String sql = "SELECT id, nombre, email, telefono FROM " + tabla + " WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JsonObject usuario = new JsonObject();
                    usuario.addProperty("id", rs.getInt("id"));
                    usuario.addProperty("nombre", rs.getString("nombre"));
                    usuario.addProperty("email", rs.getString("email"));
                    usuario.addProperty("telefono", rs.getString("telefono"));
                    
                    
                    // Generar URL del avatar
                    String email = rs.getString("email");
                    String avatarUrl = generarUrlAvatar(email, 200);
                    usuario.addProperty("avatarUrl", avatarUrl);
                    
                    JsonObject respuesta = new JsonObject();
                    respuesta.addProperty("success", true);
                    respuesta.add("usuario", usuario);
                    
                    response.getWriter().write(gson.toJson(respuesta));
                    
                    System.out.println("✅ Perfil obtenido: " + rs.getString("nombre"));
                } else {
                    enviarError(response, "Usuario no encontrado");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener perfil: " + e.getMessage());
            e.printStackTrace();
            enviarError(response, "Error al consultar el perfil");
        }
    }
    
    /**
     * Obtiene solo la URL del avatar del usuario
     * Parámetros opcionales: size (tamaño en píxeles, default 200)
     */
    private void obtenerUrlAvatar(HttpServletRequest request, HttpServletResponse response,
                                  HttpSession session) throws IOException {
        
        String email = (String) session.getAttribute("email");
        
        if (email == null || email.isEmpty()) {
            email = "default@example.com";
        }
        
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
        
        String avatarUrl = generarUrlAvatar(email, size);
        
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("success", true);
        respuesta.addProperty("avatarUrl", avatarUrl);
        
        response.getWriter().write(gson.toJson(respuesta));
    }
    
    /**
     * Verifica si un email está disponible (no está en uso por otro usuario)
     */
    private void verificarEmailDisponible(HttpServletRequest request, HttpServletResponse response,
                                          HttpSession session) throws IOException {
        
        String nuevoEmail = request.getParameter("email");
        Integer userId = (Integer) session.getAttribute("userId");
        
        if (nuevoEmail == null || nuevoEmail.trim().isEmpty()) {
            enviarError(response, "Email no proporcionado");
            return;
        }
        
        nuevoEmail = nuevoEmail.trim().toLowerCase();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM usuarios WHERE email = ? AND id != ?")) {
            
            ps.setString(1, nuevoEmail);
            ps.setInt(2, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                
                JsonObject respuesta = new JsonObject();
                respuesta.addProperty("success", true);
                respuesta.addProperty("disponible", count == 0);
                
                response.getWriter().write(gson.toJson(respuesta));
            }
            
        } catch (SQLException e) {
            System.err.println("Error al verificar email: " + e.getMessage());
            enviarError(response, "Error al verificar disponibilidad del email");
        }
    }
    
    // ============================================
    // MÉTODOS POST - ACTUALIZACIÓN DE DATOS
    // ============================================
    
    /**
     * Actualiza datos personales del usuario (nombre, teléfono)
     * NO actualiza email ni password (tienen sus propios métodos)
     */
    private void actualizarDatosPersonales(HttpServletRequest request, HttpServletResponse response,
                                           HttpSession session) throws IOException {
        
        Integer userId = (Integer) session.getAttribute("userId");
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        
        String nombre = request.getParameter("nombre");
        String telefono = request.getParameter("telefono");
        
        // Validaciones
        if (nombre == null || nombre.trim().isEmpty()) {
            enviarError(response, "El nombre es obligatorio");
            return;
        }
        
        if (telefono == null || telefono.trim().isEmpty()) {
            enviarError(response, "El teléfono es obligatorio");
            return;
        }
        
        nombre = nombre.trim();
        telefono = telefono.trim();
        
        // Validar formato de teléfono (básico)
        if (!telefono.matches("^[+]?[0-9\\s-]+$")) {
            enviarError(response, "Formato de teléfono inválido");
            return;
        }
        
        String tabla = (esAdmin != null && esAdmin) ? "usuarios_admin" : "usuarios";
        String sql = "UPDATE " + tabla + " SET nombre = ?, telefono = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, nombre);
            ps.setString(2, telefono);
            ps.setInt(3, userId);
            
            int filasAfectadas = ps.executeUpdate();
            
            if (filasAfectadas > 0) {
                // Actualizar sesión
                session.setAttribute("usuario", nombre);
                session.setAttribute("telefono", telefono);
                
                JsonObject respuesta = new JsonObject();
                respuesta.addProperty("success", true);
                respuesta.addProperty("mensaje", "Datos actualizados correctamente");
                
                response.getWriter().write(gson.toJson(respuesta));
                
                System.out.println("✅ Datos actualizados para usuario ID: " + userId);
            } else {
                enviarError(response, "No se pudo actualizar la información");
            }
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar datos: " + e.getMessage());
            e.printStackTrace();
            enviarError(response, "Error al actualizar la información");
        }
    }
    
    /**
     * Cambia la contraseña del usuario
     * Requiere: passwordActual, passwordNueva
     */
    private void cambiarPassword(HttpServletRequest request, HttpServletResponse response,
                                 HttpSession session) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        
        Integer userId = (Integer) session.getAttribute("userId");
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        
        String passwordActual = request.getParameter("passwordActual");
        String passwordNueva = request.getParameter("passwordNueva");
        
        // Validaciones
        if (passwordActual == null || passwordActual.isEmpty()) {
            enviarError(response, "Debe proporcionar su contraseña actual");
            return;
        }
        
        if (passwordNueva == null || passwordNueva.isEmpty()) {
            enviarError(response, "Debe proporcionar una contraseña nueva");
            return;
        }
        
        if (passwordNueva.length() < 6) {
            enviarError(response, "La contraseña nueva debe tener al menos 6 caracteres");
            return;
        }
        
        String tabla = (esAdmin != null && esAdmin) ? "usuarios_admin" : "usuarios";
        
        try (Connection conn = dbManager.getConnection()) {
            
            // 1. Verificar contraseña actual
            String sqlVerificar = "SELECT password_hash, salt FROM " + tabla + " WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlVerificar)) {
                ps.setInt(1, userId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        enviarError(response, "Usuario no encontrado");
                        return;
                    }
                    
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    
                    // Verificar contraseña actual
                    if (!PasswordHasher.verificarPassword(passwordActual, storedHash, salt)) {
                        enviarError(response, "La contraseña actual es incorrecta");
                        System.out.println("⚠️ Intento de cambio de contraseña con password incorrecta");
                        return;
                    }
                }
            }
            
            // 2. Generar nueva contraseña hasheada
            byte[] newSalt = PasswordHasher.generarSalt();
            String newSaltBase64 = Base64.getEncoder().encodeToString(newSalt);
            String newPasswordHash = PasswordHasher.hashPassword(passwordNueva, newSalt);
            
            // 3. Actualizar en base de datos
            String sqlActualizar = "UPDATE " + tabla + " SET password_hash = ?, salt = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlActualizar)) {
                ps.setString(1, newPasswordHash);
                ps.setString(2, newSaltBase64);
                ps.setInt(3, userId);
                
                int filasAfectadas = ps.executeUpdate();
                
                if (filasAfectadas > 0) {
                    JsonObject respuesta = new JsonObject();
                    respuesta.addProperty("success", true);
                    respuesta.addProperty("mensaje", "Contraseña cambiada correctamente");
                    
                    response.getWriter().write(gson.toJson(respuesta));
                    
                    System.out.println("✅ Contraseña cambiada para usuario ID: " + userId);
                } else {
                    enviarError(response, "No se pudo cambiar la contraseña");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al cambiar contraseña: " + e.getMessage());
            e.printStackTrace();
            enviarError(response, "Error al cambiar la contraseña");
        }
    }
    
    /**
     * Actualiza el email del usuario
     * Importante: Al cambiar el email, cambia el avatar de Gravatar
     */
    private void actualizarEmail(HttpServletRequest request, HttpServletResponse response,
                                 HttpSession session) throws IOException {
        
        Integer userId = (Integer) session.getAttribute("userId");
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        
        String nuevoEmail = request.getParameter("email");
        String password = request.getParameter("password"); // Requiere confirmar con password
        
        // Validaciones
        if (nuevoEmail == null || nuevoEmail.trim().isEmpty()) {
            enviarError(response, "Debe proporcionar un email");
            return;
        }
        
        if (password == null || password.isEmpty()) {
            enviarError(response, "Debe confirmar con su contraseña");
            return;
        }
        
        nuevoEmail = nuevoEmail.trim().toLowerCase();
        
        // Validar formato de email
        if (!nuevoEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            enviarError(response, "Formato de email inválido");
            return;
        }
        
        String tabla = (esAdmin != null && esAdmin) ? "usuarios_admin" : "usuarios";
        
        try (Connection conn = dbManager.getConnection()) {
            
            // 1. Verificar contraseña
            String sqlVerificar = "SELECT password_hash, salt FROM " + tabla + " WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlVerificar)) {
                ps.setInt(1, userId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        enviarError(response, "Usuario no encontrado");
                        return;
                    }
                    
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    
                    if (!PasswordHasher.verificarPassword(password, storedHash, salt)) {
                        enviarError(response, "Contraseña incorrecta");
                        return;
                    }
                }
            }
            
            // 2. Verificar que el email no esté en uso
            String sqlCheckEmail = "SELECT COUNT(*) FROM " + tabla + " WHERE email = ? AND id != ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlCheckEmail)) {
                ps.setString(1, nuevoEmail);
                ps.setInt(2, userId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        enviarError(response, "Este email ya está en uso");
                        return;
                    }
                }
            }
            
            // 3. Actualizar email
            String sqlActualizar = "UPDATE " + tabla + " SET email = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlActualizar)) {
                ps.setString(1, nuevoEmail);
                ps.setInt(2, userId);
                
                int filasAfectadas = ps.executeUpdate();
                
                if (filasAfectadas > 0) {
                    // Actualizar sesión
                    session.setAttribute("email", nuevoEmail);
                    
                    // Generar nueva URL de avatar
                    String nuevoAvatar = generarUrlAvatar(nuevoEmail, 200);
                    
                    JsonObject respuesta = new JsonObject();
                    respuesta.addProperty("success", true);
                    respuesta.addProperty("mensaje", "Email actualizado correctamente");
                    respuesta.addProperty("nuevoEmail", nuevoEmail);
                    respuesta.addProperty("nuevoAvatar", nuevoAvatar);
                    
                    response.getWriter().write(gson.toJson(respuesta));
                    
                    System.out.println("✅ Email actualizado para usuario ID: " + userId + " -> " + nuevoEmail);
                } else {
                    enviarError(response, "No se pudo actualizar el email");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar email: " + e.getMessage());
            e.printStackTrace();
            
            if (e.getMessage().contains("Duplicate entry")) {
                enviarError(response, "Este email ya está en uso");
            } else {
                enviarError(response, "Error al actualizar el email");
            }
        }
    }
    
    // ============================================
    // MÉTODOS AUXILIARES
    // ============================================
    
    /**
     * Genera la URL del avatar de Gravatar a partir de un email
     */
    private String generarUrlAvatar(String email, int size) {
        if (email == null || email.isEmpty()) {
            return GRAVATAR_URL + "00000000000000000000000000000000?d=mp&s=" + size;
        }
        
        String emailHash = generarMD5(email.trim().toLowerCase());
        return GRAVATAR_URL + emailHash + "?d=mp&s=" + size;
    }
    
    /**
     * Genera hash MD5 de un string (requerido por Gravatar)
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
     * Envía respuesta de error en formato JSON
     */
    private void enviarError(HttpServletResponse response, String mensaje) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", mensaje);
        
        response.getWriter().write(gson.toJson(error));
    }
}