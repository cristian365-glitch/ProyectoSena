package UX;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import com.conexiones.DatabaseManager;
import com.servicios.EmailService;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Servlet para recuperación de contraseña
 * @author Calixto
 */
@WebServlet(name = "RecuperarPasswordServlet", urlPatterns = {"/RecuperarPasswordServlet"})
public class RecuperarPasswordServlet extends HttpServlet {
    
    private Gson gson = new Gson();
    private DatabaseManager dbManager = DatabaseManager.getInstance();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        switch (action != null ? action : "") {
            case "solicitar":
                solicitarRecuperacion(request, response);
                break;
            case "verificar":
                verificarCodigo(request, response);
                break;
            case "resetear":
                resetearPassword(request, response);
                break;
            default:
                enviarErrorJSON(response, "Acción no válida");
        }
    }
    
    /**
     * Solicitar recuperación de contraseña - Envía código por email
     */
    private void solicitarRecuperacion(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        String email = request.getParameter("email");
        
        if (email == null || email.trim().isEmpty()) {
            enviarErrorJSON(response, "El email es requerido");
            return;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            // Verificar que el email existe
            String sqlCheck = "SELECT usuario FROM usuarios WHERE email = ?";
            stmt = conn.prepareStatement(sqlCheck);
            stmt.setString(1, email);
            rs = stmt.executeQuery();
            
            if (!rs.next()) {
                // Por seguridad, no revelar si el email existe o no
                enviarSuccessJSON(response, "Si el email existe, recibirás un código de verificación");
                return;
            }
            
            DatabaseManager.closeResources(rs, stmt);
            
            // Generar código de 6 dígitos
            String codigo = String.format("%06d", (int)(Math.random() * 1000000));
            
            // Guardar el código en la base de datos con expiración de 15 minutos
            String sqlInsert = "INSERT INTO codigos_recuperacion (email, codigo, fecha_expiracion) " +
                              "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE)) " +
                              "ON DUPLICATE KEY UPDATE codigo = ?, fecha_expiracion = DATE_ADD(NOW(), INTERVAL 15 MINUTE)";
            
            stmt = conn.prepareStatement(sqlInsert);
            stmt.setString(1, email);
            stmt.setString(2, codigo);
            stmt.setString(3, codigo);
            stmt.executeUpdate();
            
            // Enviar email con el código
            boolean emailEnviado = EmailService.enviarCodigoRecuperacion(email, codigo);
            
            if (emailEnviado) {
                enviarSuccessJSON(response, "Código de verificación enviado a tu email");
            } else {
                enviarErrorJSON(response, "Error al enviar el email. Intenta de nuevo");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarErrorJSON(response, "Error al procesar la solicitud");
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Verificar el código de recuperación
     */
    private void verificarCodigo(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        String email = request.getParameter("email");
        String codigo = request.getParameter("codigo");
        
        if (email == null || codigo == null) {
            enviarErrorJSON(response, "Email y código son requeridos");
            return;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            String sql = "SELECT codigo, fecha_expiracion FROM codigos_recuperacion " +
                        "WHERE email = ? AND usado = FALSE";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String codigoGuardado = rs.getString("codigo");
                Timestamp expiracion = rs.getTimestamp("fecha_expiracion");
                
                // Verificar si el código ha expirado
                if (expiracion.before(new Timestamp(System.currentTimeMillis()))) {
                    enviarErrorJSON(response, "El código ha expirado. Solicita uno nuevo");
                    return;
                }
                
                // Verificar si el código coincide
                if (codigo.equals(codigoGuardado)) {
                    // Generar token único para resetear la contraseña
                    String token = UUID.randomUUID().toString();
                    
                    DatabaseManager.closeResources(rs, stmt);
                    
                    // Actualizar con el token
                    String sqlUpdate = "UPDATE codigos_recuperacion SET token = ? WHERE email = ?";
                    stmt = conn.prepareStatement(sqlUpdate);
                    stmt.setString(1, token);
                    stmt.setString(2, email);
                    stmt.executeUpdate();
                    
                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("success", true);
                    resultado.put("token", token);
                    resultado.put("mensaje", "Código verificado correctamente");
                    enviarJSON(response, resultado);
                } else {
                    enviarErrorJSON(response, "Código incorrecto");
                }
            } else {
                enviarErrorJSON(response, "No se encontró un código válido");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarErrorJSON(response, "Error al verificar el código");
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Resetear la contraseña con el token validado
     */
    private void resetearPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        String email = request.getParameter("email");
        String token = request.getParameter("token");
        String nuevaPassword = request.getParameter("password");
        
        if (email == null || token == null || nuevaPassword == null) {
            enviarErrorJSON(response, "Datos incompletos");
            return;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            // Verificar el token
            String sqlVerify = "SELECT token FROM codigos_recuperacion " +
                              "WHERE email = ? AND token = ? AND usado = FALSE " +
                              "AND fecha_expiracion > NOW()";
            
            stmt = conn.prepareStatement(sqlVerify);
            stmt.setString(1, email);
            stmt.setString(2, token);
            rs = stmt.executeQuery();
            
            if (!rs.next()) {
                enviarErrorJSON(response, "Token inválido o expirado");
                return;
            }
            
            DatabaseManager.closeResources(rs, stmt);
            
            // Actualizar la contraseña
            String nuevoHash = BCrypt.hashpw(nuevaPassword, BCrypt.gensalt());
            String sqlUpdate = "UPDATE usuarios SET password = ? WHERE email = ?";
            
            stmt = conn.prepareStatement(sqlUpdate);
            stmt.setString(1, nuevoHash);
            stmt.setString(2, email);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                DatabaseManager.closeResources(stmt);
                
                // Marcar el código como usado
                String sqlMarkUsed = "UPDATE codigos_recuperacion SET usado = TRUE WHERE email = ?";
                stmt = conn.prepareStatement(sqlMarkUsed);
                stmt.setString(1, email);
                stmt.executeUpdate();
                
                enviarSuccessJSON(response, "Contraseña actualizada correctamente");
            } else {
                enviarErrorJSON(response, "No se pudo actualizar la contraseña");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarErrorJSON(response, "Error al resetear la contraseña");
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
     * Enviar respuesta JSON de éxito simple
     */
    private void enviarSuccessJSON(HttpServletResponse response, String mensaje) throws IOException {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("success", true);
        resultado.put("mensaje", mensaje);
        enviarJSON(response, resultado);
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
        return "Servlet para recuperación de contraseña";
    }
}