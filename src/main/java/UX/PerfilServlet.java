package UX;

import com.conexiones.DatabaseManager;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.*;
import java.security.MessageDigest;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Servlet para gestionar el perfil de usuario
 * @author Calixto
 */
@WebServlet("/PerfilServlet")
public class PerfilServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            dbManager = DatabaseManager.getInstance();
            
            if (!dbManager.testConnection()) {
                throw new ServletException("No se puede conectar a la base de datos");
            }
            System.out.println("✅ PerfilServlet inicializado correctamente");
        } catch (Exception e) {
            System.err.println("❌ Error al inicializar PerfilServlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Error al inicializar servlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if ("obtenerPerfil".equals(action)) {
            obtenerDatosPerfil(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Acción no válida");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        switch (action != null ? action : "") {
            case "actualizarDatos":
                actualizarDatosPersonales(request, response);
                break;
            case "actualizarEmail":
                actualizarEmail(request, response);
                break;
            case "cambiarPassword":
                cambiarPassword(request, response);
                break;
            default:
                enviarErrorJSON(response, "Acción no válida");
        }
    }
    
    /**
     * Obtener datos del perfil del usuario logueado
     */
    private void obtenerDatosPerfil(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("logueado") == null) {
            enviarError(response, out, "No hay sesión activa", 401);
            return;
        }
        
        String emailUsuario = (String) session.getAttribute("email");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            String sql = "SELECT id, nombre, email, telefono, fecha_ultimo_login " +
                        "FROM usuarios WHERE email = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, emailUsuario);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String email = rs.getString("email");
                
                // Generar hash MD5 del email para Gravatar
                String avatarUrl = generarGravatarURL(email);
                
                // Formatear fecha de registro
                Date fechaUltimoLogin = rs.getDate("fecha_ultimo_login");
                String fechaRegistroStr = fechaUltimoLogin != null ? fechaUltimoLogin.toString() : null;
                
                // Crear objeto usuario con los datos
                JSONObject usuario = new JSONObject();
                usuario.put("usuario", rs.getString("nombre"));
                usuario.put("email", email);
                usuario.put("nombre", rs.getString("nombre"));
                usuario.put("telefono", rs.getString("telefono"));
                usuario.put("fechaRegistro", fechaRegistroStr);  // Formato: "YYYY-MM-DD"
                usuario.put("avatarUrl", avatarUrl);  // URL del avatar de Gravatar
                
                // Crear respuesta con la estructura correcta
                JSONObject perfil = new JSONObject();
                perfil.put("success", true);
                perfil.put("usuario", usuario);
                
                out.print(perfil.toString());
                out.flush();
                
            } else {
                enviarError(response, out, "Usuario no encontrado", 404);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, out, "Error al consultar la base de datos", 500);
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Generar URL del avatar de Gravatar basado en el email
     */
    private String generarGravatarURL(String email) {
        try {
            // Convertir email a minúsculas y quitar espacios
            String emailLimpio = email.trim().toLowerCase();
            
            // Generar hash MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(emailLimpio.getBytes("UTF-8"));
            
            // Convertir a hexadecimal
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            
            String hash = sb.toString();
            
            // Retornar URL de Gravatar
            return "https://www.gravatar.com/avatar/" + hash + "?d=mp&s=200";
            
        } catch (Exception e) {
            e.printStackTrace();
            // En caso de error, retornar avatar por defecto
            return "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&s=200";
        }
    }
    
    /**
     * Actualizar datos personales (nombre y teléfono)
     */
    private void actualizarDatosPersonales(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("logueado") == null) {
            enviarError(response, out, "No hay sesión activa", 401);
            return;
        }
        
        String emailUsuario = (String) session.getAttribute("email");
        String nombre = request.getParameter("nombre");
        String telefono = request.getParameter("telefono");
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dbManager.getConnection();
            
            String sql = "UPDATE usuarios SET nombre = ?, telefono = ? WHERE email = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nombre);
            stmt.setString(2, telefono);
            stmt.setString(3, emailUsuario);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                JSONObject resultado = new JSONObject();
                resultado.put("success", true);
                resultado.put("mensaje", "Datos actualizados correctamente");
                
                out.print(resultado.toString());
                out.flush();
            } else {
                enviarError(response, out, "No se pudo actualizar los datos", 400);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, out, "Error al actualizar los datos", 500);
        } finally {
            DatabaseManager.closeResources(stmt, conn);
        }
    }
    
    /**
     * Actualizar el email del usuario
     */
    private void actualizarEmail(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("logueado") == null) {
            enviarError(response, out, "No hay sesión activa", 401);
            return;
        }
        
        String emailActual = (String) session.getAttribute("email");
        String nuevoEmail = request.getParameter("email");
        String password = request.getParameter("password");
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            // Verificar la contraseña actual
            String sqlVerificar = "SELECT password_hash FROM usuarios WHERE email = ?";
            stmt = conn.prepareStatement(sqlVerificar);
            stmt.setString(1, emailActual);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String passwordHash = rs.getString("password_hash");
                
                if (!BCrypt.checkpw(password, passwordHash)) {
                    enviarError(response, out, "Contraseña incorrecta", 403);
                    return;
                }
            } else {
                enviarError(response, out, "Usuario no encontrado", 404);
                return;
            }
            
            DatabaseManager.closeResources(rs, stmt);
            
            // Verificar que el nuevo email no esté en uso
            String sqlCheck = "SELECT COUNT(*) FROM usuarios WHERE email = ?";
            stmt = conn.prepareStatement(sqlCheck);
            stmt.setString(1, nuevoEmail);
            rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                enviarError(response, out, "El email ya está en uso", 400);
                return;
            }
            
            DatabaseManager.closeResources(rs, stmt);
            
            // Actualizar el email
            String sqlUpdate = "UPDATE usuarios SET email = ? WHERE email = ?";
            stmt = conn.prepareStatement(sqlUpdate);
            stmt.setString(1, nuevoEmail);
            stmt.setString(2, emailActual);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Actualizar la sesión
                session.setAttribute("email", nuevoEmail);
                
                JSONObject resultado = new JSONObject();
                resultado.put("success", true);
                resultado.put("mensaje", "Email actualizado correctamente");
                
                out.print(resultado.toString());
                out.flush();
            } else {
                enviarError(response, out, "No se pudo actualizar el email", 400);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, out, "Error al actualizar el email", 500);
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Cambiar la contraseña del usuario
     */
    private void cambiarPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("logueado") == null) {
            enviarError(response, out, "No hay sesión activa", 401);
            return;
        }
        
        String emailUsuario = (String) session.getAttribute("email");
        String passwordActual = request.getParameter("passwordActual");
        String passwordNueva = request.getParameter("passwordNueva");
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            // Verificar la contraseña actual
            String sqlVerificar = "SELECT password_hash FROM usuarios WHERE email = ?";
            stmt = conn.prepareStatement(sqlVerificar);
            stmt.setString(1, emailUsuario);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String passwordHash = rs.getString("password_hash");
                
                if (!BCrypt.checkpw(passwordActual, passwordHash)) {
                    enviarError(response, out, "La contraseña actual es incorrecta", 403);
                    return;
                }
            } else {
                enviarError(response, out, "Usuario no encontrado", 404);
                return;
            }
            
            DatabaseManager.closeResources(rs, stmt);
            
            // Actualizar con la nueva contraseña
            String nuevoHash = BCrypt.hashpw(passwordNueva, BCrypt.gensalt());
            String sqlUpdate = "UPDATE usuarios SET password_hash = ? WHERE email = ?";
            
            stmt = conn.prepareStatement(sqlUpdate);
            stmt.setString(1, nuevoHash);
            stmt.setString(2, emailUsuario);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                JSONObject resultado = new JSONObject();
                resultado.put("success", true);
                resultado.put("mensaje", "Contraseña actualizada correctamente");
                
                out.print(resultado.toString());
                out.flush();
            } else {
                enviarError(response, out, "No se pudo actualizar la contraseña", 400);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, out, "Error al actualizar la contraseña", 500);
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Método auxiliar para enviar errores en formato JSON
     */
    private void enviarError(HttpServletResponse response, PrintWriter out, String mensaje, int statusCode) 
            throws IOException {
        response.setStatus(statusCode);
        JSONObject error = new JSONObject();
        error.put("success", false);
        error.put("error", mensaje);
        
        out.print(error.toString());
        out.flush();
    }
    
    /**
     * Enviar respuesta JSON de error (versión sin PrintWriter)
     */
    private void enviarErrorJSON(HttpServletResponse response, String mensaje) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        enviarError(response, out, mensaje, 400);
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestión de perfil de usuario";
    }
}