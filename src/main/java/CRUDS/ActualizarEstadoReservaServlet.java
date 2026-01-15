package CRUDS;

import com.conexiones.DatabaseManager;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ActualizarEstadoReservaServlet")
public class ActualizarEstadoReservaServlet extends HttpServlet {
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
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String reservaIdStr = request.getParameter("reserva_id");
        String nuevoEstado = request.getParameter("estado");
        
        if (reservaIdStr == null || reservaIdStr.trim().isEmpty() ||
            nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            enviarRespuesta(response, false, "Parámetros incompletos");
            return;
        }
        
        // Validar estado permitido
        if (!esEstadoValido(nuevoEstado)) {
            enviarRespuesta(response, false, "Estado no válido");
            return;
        }
        
        try {
            int reservaId = Integer.parseInt(reservaIdStr);
            
            Connection conn = null;
            PreparedStatement ps = null;
            
            try {
                conn = dbManager.getConnection();
                
                String sql = "UPDATE reservas SET estado = ? WHERE id = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, nuevoEstado);
                ps.setInt(2, reservaId);
                
                int filasActualizadas = ps.executeUpdate();
                
                if (filasActualizadas > 0) {
                    enviarRespuesta(response, true, "Estado actualizado correctamente");
                } else {
                    enviarRespuesta(response, false, "Reserva no encontrada");
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                enviarRespuesta(response, false, "Error al actualizar la base de datos");
            } finally {
                DatabaseManager.closeResources(null, ps, conn);
            }
            
        } catch (NumberFormatException e) {
            enviarRespuesta(response, false, "ID de reserva inválido");
        }
    }
    
    private boolean esEstadoValido(String estado) {
        return estado.equals("pendiente") || 
               estado.equals("pendiente_verificacion") ||
               estado.equals("confirmada") || 
               estado.equals("cancelada");
    }
    
    private void enviarRespuesta(HttpServletResponse response, boolean success, String mensaje) 
            throws IOException {
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("success", success);
        resultado.put("mensaje", mensaje);
        out.print(gson.toJson(resultado));
        out.flush();
    }
}