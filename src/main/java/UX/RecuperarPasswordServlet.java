package UX;

import com.conexiones.DatabaseManager;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;
import org.json.JSONObject;

/**
 * Servlet para recuperación de contraseña
 * @author Calixto
 */
@WebServlet("/RecuperarPasswordServlet")
public class RecuperarPasswordServlet extends HttpServlet {
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
            System.out.println("✅ RecuperarPasswordServlet inicializado correctamente");
        } catch (Exception e) {
            System.err.println("❌ Error al inicializar RecuperarPasswordServlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Error al inicializar servlet", e);
        }
    }

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
                enviarErrorJSON(response, "Acción no válida", 400);
        }
    }
    
    /**
     * Solicitar recuperación de contraseña - Envía código por email
     */
    private void solicitarRecuperacion(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String email = request.getParameter("email");
        
        if (email == null || email.trim().isEmpty()) {
            enviarError(response, out, "El email es requerido", 400);
            return;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            // Verificar que el email existe
            String sqlCheck = "SELECT nombre FROM usuarios WHERE email = ?";
            stmt = conn.prepareStatement(sqlCheck);
            stmt.setString(1, email);
            rs = stmt.executeQuery();
            
            if (!rs.next()) {
                // Por seguridad, no revelar si el email existe o no
                enviarSuccess(response, out, "Si el email existe, recibirás un código de verificación");
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
                enviarSuccess(response, out, "Código de verificación enviado a tu email");
            } else {
                enviarError(response, out, "Error al enviar el email. Intenta de nuevo", 500);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, out, "Error al procesar la solicitud", 500);
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Verificar el código de recuperación
     */
    private void verificarCodigo(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String email = request.getParameter("email");
        String codigo = request.getParameter("codigo");
        
        if (email == null || codigo == null) {
            enviarError(response, out, "Email y código son requeridos", 400);
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
                    enviarError(response, out, "El código ha expirado. Solicita uno nuevo", 400);
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
                    
                    JSONObject resultado = new JSONObject();
                    resultado.put("success", true);
                    resultado.put("token", token);
                    resultado.put("mensaje", "Código verificado correctamente");
                    
                    out.print(resultado.toString());
                    out.flush();
                } else {
                    enviarError(response, out, "Código incorrecto", 400);
                }
            } else {
                enviarError(response, out, "No se encontró un código válido", 404);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, out, "Error al verificar el código", 500);
        } finally {
            DatabaseManager.closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Resetear la contraseña con el token validado
     */
    private void resetearPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String email = request.getParameter("email");
        String token = request.getParameter("token");
        String nuevaPassword = request.getParameter("password");
        
        if (email == null || token == null || nuevaPassword == null) {
            enviarError(response, out, "Datos incompletos", 400);
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
                enviarError(response, out, "Token inválido o expirado", 400);
                return;
            }
            
            DatabaseManager.closeResources(rs, stmt);
            
            // Actualizar la contraseña
            String nuevoHash = BCrypt.hashpw(nuevaPassword, BCrypt.gensalt());
            String sqlUpdate = "UPDATE usuarios SET password_hash = ? WHERE email = ?";
            
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
                
                enviarSuccess(response, out, "Contraseña actualizada correctamente");
            } else {
                enviarError(response, out, "No se pudo actualizar la contraseña", 500);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, out, "Error al resetear la contraseña", 500);
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
    private void enviarErrorJSON(HttpServletResponse response, String mensaje, int statusCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        enviarError(response, out, mensaje, statusCode);
    }
    
    /**
     * Enviar respuesta JSON de éxito
     */
    private void enviarSuccess(HttpServletResponse response, PrintWriter out, String mensaje) 
            throws IOException {
        JSONObject resultado = new JSONObject();
        resultado.put("success", true);
        resultado.put("mensaje", mensaje);
        
        out.print(resultado.toString());
        out.flush();
    }

    @Override
    public String getServletInfo() {
        return "Servlet para recuperación de contraseña";
    }
}