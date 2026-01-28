package UX;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.google.gson.Gson;
import com.conexiones.DatabaseManager;
import com.conexiones.DatabaseManager;
import com.conexiones.DatabaseManager;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Servlet para gestionar el perfil de usuario
 * @author Calixto
 */
@WebServlet(name = "PerfilServlet", urlPatterns = {"/PerfilServlet"})
public class PerfilServlet extends HttpServlet {
    
    private Gson gson = new Gson();
    private DatabaseManager dbManager = DatabaseManager.getInstance();

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
        
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("usuario") == null) {
            enviarErrorJSON(response, "No hay sesión activa");
            return;
        }
        
        String emailUsuario = (String) session.getAttribute("email");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            String sql = "SELECT usuario, email, nombre, telefono, fecha_registro, esAdmin " +
                        "FROM usuarios WHERE email = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, emailUsuario);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Crear objeto usuario con los datos
                Map<String, Object> usuario = new HashMap<>();
                usuario.put("usuario", rs.getString("usuario"));
                usuario.put("email", rs.getString("email"));
                usuario.put("nombre", rs.getString("nombre"));
                usuario.put("telefono", rs.getString("telefono"));
                usuario.put("miembroDesde", rs.getDate("fecha_registro"));
                usuario.put("esAdmin", rs.getBoolean("esAdmin"));
                
                // Crear respuesta con la estructura correcta
                Map<String, Object> perfil = new HashMap<>();
                perfil.put("success", true);
                perfil.put("usuario", usuario);  // ⭐ El HTML espera data.usuario
                
                enviarJSON(response, perfil);
            } else {
                enviarErrorJSON(response, "Usuario no encontrado");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarErrorJSON(response, "Error al consultar la base de datos");
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Actualizar datos personales (nombre y teléfono)
     */
    private void actualizarDatosPersonales(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("usuario") == null) {
            enviarErrorJSON(response, "No hay sesión activa");
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
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("success", true);
                resultado.put("mensaje", "Datos actualizados correctamente");
                enviarJSON(response, resultado);
            } else {
                enviarErrorJSON(response, "No se pudo actualizar los datos");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarErrorJSON(response, "Error al actualizar los datos");
        } finally {
            DatabaseManager.closeResources(stmt, conn);
        }
    }
    
    /**
     * Actualizar el email del usuario
     */
    private void actualizarEmail(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("usuario") == null) {
            enviarErrorJSON(response, "No hay sesión activa");
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
            String sqlVerificar = "SELECT password FROM usuarios WHERE email = ?";
            stmt = conn.prepareStatement(sqlVerificar);
            stmt.setString(1, emailActual);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String passwordHash = rs.getString("password");
                
                if (!BCrypt.checkpw(password, passwordHash)) {
                    enviarErrorJSON(response, "Contraseña incorrecta");
                    return;
                }
            } else {
                enviarErrorJSON(response, "Usuario no encontrado");
                return;
            }
            
            DatabaseManager.closeResources(rs, stmt);
            
            // Verificar que el nuevo email no esté en uso
            String sqlCheck = "SELECT COUNT(*) FROM usuarios WHERE email = ?";
            stmt = conn.prepareStatement(sqlCheck);
            stmt.setString(1, nuevoEmail);
            rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                enviarErrorJSON(response, "El email ya está en uso");
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
                
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("success", true);
                resultado.put("mensaje", "Email actualizado correctamente");
                enviarJSON(response, resultado);
            } else {
                enviarErrorJSON(response, "No se pudo actualizar el email");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarErrorJSON(response, "Error al actualizar el email");
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Cambiar la contraseña del usuario
     */
    private void cambiarPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("usuario") == null) {
            enviarErrorJSON(response, "No hay sesión activa");
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
            String sqlVerificar = "SELECT password FROM usuarios WHERE email = ?";
            stmt = conn.prepareStatement(sqlVerificar);
            stmt.setString(1, emailUsuario);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String passwordHash = rs.getString("password");
                
                if (!BCrypt.checkpw(passwordActual, passwordHash)) {
                    enviarErrorJSON(response, "La contraseña actual es incorrecta");
                    return;
                }
            } else {
                enviarErrorJSON(response, "Usuario no encontrado");
                return;
            }
            
            DatabaseManager.closeResources(rs, stmt);
            
            // Actualizar con la nueva contraseña
            String nuevoHash = BCrypt.hashpw(passwordNueva, BCrypt.gensalt());
            String sqlUpdate = "UPDATE usuarios SET password = ? WHERE email = ?";
            
            stmt = conn.prepareStatement(sqlUpdate);
            stmt.setString(1, nuevoHash);
            stmt.setString(2, emailUsuario);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("success", true);
                resultado.put("mensaje", "Contraseña actualizada correctamente");
                enviarJSON(response, resultado);
            } else {
                enviarErrorJSON(response, "No se pudo actualizar la contraseña");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarErrorJSON(response, "Error al actualizar la contraseña");
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Enviar respuesta JSON exitosa
     */
    private void enviarJSON(HttpServletResponse response, Map<String, Object> data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(data));
        out.flush();
    }
    
    /**
     * Enviar respuesta JSON de error
     */
    private void enviarErrorJSON(HttpServletResponse response, String mensaje) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", mensaje);
        out.print(gson.toJson(error));
        out.flush();
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestión de perfil de usuario";
    }
}