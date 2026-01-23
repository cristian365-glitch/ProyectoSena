package com.conexiones;

import untils.PasswordHasher;
import java.io.IOException;
import java.io.PrintWriter;
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

@WebServlet("/RegistroServlet")
public class RegistroServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    
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
        
        String nombre = request.getParameter("nombre");
        String email = request.getParameter("email");
        String telefono = request.getParameter("telefono");
        String password = request.getParameter("password");
        
        System.out.println("📝 Intento de registro: " + email);
        
        // Validaciones
        if (nombre == null || nombre.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            telefono == null || telefono.trim().isEmpty() ||
            password == null || password.isEmpty()) {
            
            System.out.println("❌ Campos vacíos");
            response.sendRedirect("/login/Login.html?error=campos_vacios");
            return;
        }
        
        // Limpiar datos
        nombre = nombre.trim();
        email = email.trim().toLowerCase();
        telefono = telefono.trim();
        
        // Validar formato de email
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            System.out.println("❌ Email inválido: " + email);
            response.sendRedirect("/login/Login.html?error=email_invalido");
            return;
        }
        
        // Validar longitud mínima de contraseña
        if (password.length() < 6) {
            System.out.println("❌ Contraseña muy corta");
            response.sendRedirect("/login/Login.html?error=password_corta");
            return;
        }
        
        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psInsert = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            
            // Verificar si el email ya existe
            String checkSql = "SELECT COUNT(*) FROM usuarios WHERE email = ?";
            psCheck = conn.prepareStatement(checkSql);
            psCheck.setString(1, email);
            rs = psCheck.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("⚠️ Email ya existe: " + email);
                response.sendRedirect("/login/Login.html?error=email_existe");
                return;
            }
            
            System.out.println("🔐 Generando hash de contraseña...");
            
            // GENERAR SALT Y HASH DE LA CONTRASEÑA
            byte[] salt = PasswordHasher.generarSalt();
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String passwordHash = PasswordHasher.hashPassword(password, salt);
            
            System.out.println("   Salt: " + saltBase64.substring(0, Math.min(20, saltBase64.length())) + "...");
            System.out.println("   Hash: " + passwordHash.substring(0, Math.min(20, passwordHash.length())) + "...");
            
            // Insertar nuevo usuario CON HASH
            String insertSql = "INSERT INTO usuarios (nombre, email, telefono, password_hash, salt) VALUES (?, ?, ?, ?, ?)";
            psInsert = conn.prepareStatement(insertSql);
            psInsert.setString(1, nombre);
            psInsert.setString(2, email);
            psInsert.setString(3, telefono);
            psInsert.setString(4, passwordHash);  // Hash seguro
            psInsert.setString(5, saltBase64);     // Salt único
            
            int filas = psInsert.executeUpdate();
            
            if (filas > 0) {
                conn.commit();
                
                System.out.println("✅ Usuario registrado exitosamente: " + email);
                
                // Crear sesión automáticamente
                HttpSession session = request.getSession(true);
                session.setAttribute("usuario", nombre);
                session.setAttribute("email", email);
                session.setAttribute("telefono", telefono);
                session.setAttribute("logueado", true);
                session.setAttribute("esAdmin", false);
                session.setMaxInactiveInterval(30 * 60);
                
                // Regenerar ID de sesión
                request.changeSessionId();
                
                response.sendRedirect("/index.html?registro=exitoso");
                
            } else {
                conn.rollback();
                System.out.println("❌ No se pudo insertar el usuario");
                response.sendRedirect("/login/Login.html?error=registro_fallido");
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            
            System.err.println("❌ Error SQL en registro:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
            
            if (e.getMessage().contains("Duplicate entry")) {
                response.sendRedirect("/login/Login.html?error=email_existe");
            } else {
                response.sendRedirect("/login/Login.html?error=error_sistema");
            }
            
        } catch (Exception e) {
            // Error generando hash
            System.err.println("❌ Error generando hash: " + e.getMessage());
            e.printStackTrace();
            
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            
            response.sendRedirect("/login/Login.html?error=error_sistema");
            
        } finally {
            // Cerrar recursos
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (psCheck != null) try { psCheck.close(); } catch (SQLException e) {}
            if (psInsert != null) try { psInsert.close(); } catch (SQLException e) {}
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if ("verificarSesion".equals(action)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            HttpSession session = request.getSession(false);
            PrintWriter out = response.getWriter();
            
            if (session != null && Boolean.TRUE.equals(session.getAttribute("logueado"))) {
                String nombre = (String) session.getAttribute("usuario");
                String email = (String) session.getAttribute("email");
                Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
                
                out.print("{\"logueado\": true, \"nombre\": \"" + nombre + "\", " +
                         "\"email\": \"" + email + "\", " +
                         "\"esAdmin\": " + (esAdmin != null && esAdmin) + "}");
            } else {
                out.print("{\"logueado\": false}");
            }
            
            out.flush();
        }
    }
}