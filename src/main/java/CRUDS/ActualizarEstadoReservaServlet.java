package CRUDS;

import com.conexiones.DatabaseManager;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/ActualizarEstadoReservaServlet")
public class ActualizarEstadoReservaServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // ✅ CONFIGURAR RESPONSE COMO JSON SIEMPRE
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = null;
        
        try {
            out = response.getWriter();
            
            // Verificar sesión
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("logueado") == null) {
                enviarError(out, "Sesión no válida");
                return;
            }
            
            String reservaIdStr = request.getParameter("reserva_id");
            String estado = request.getParameter("estado");
            
            System.out.println("========================================");
            System.out.println("📝 Actualizando estado de reserva");
            System.out.println("   Reserva ID: " + reservaIdStr);
            System.out.println("   Nuevo estado: " + estado);
            System.out.println("========================================");
            
            if (reservaIdStr == null || estado == null) {
                enviarError(out, "Parámetros faltantes");
                return;
            }
            
            int reservaId = Integer.parseInt(reservaIdStr);
            
            Connection conn = null;
            PreparedStatement ps = null;
            
            try {
                conn = dbManager.getConnection();
                
                String sql = "UPDATE reservas SET estado = ? WHERE id = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, estado);
                ps.setInt(2, reservaId);
                
                int filasActualizadas = ps.executeUpdate();
                
                if (filasActualizadas > 0) {
                    System.out.println("✅ Reserva actualizada correctamente");
                    System.out.println("========================================");
                    
                    Map<String, Object> respuesta = new HashMap<>();
                    respuesta.put("success", true);
                    respuesta.put("estado", estado);
                    respuesta.put("reserva_id", reservaId);
                    
                    Gson gson = new Gson();
                    out.print(gson.toJson(respuesta));
                } else {
                    System.err.println("⚠️ Reserva no encontrada");
                    enviarError(out, "Reserva no encontrada");
                }
                
            } catch (Exception e) {
                System.err.println("❌ Error al actualizar:");
                e.printStackTrace();
                enviarError(out, "Error al actualizar: " + e.getMessage());
            } finally {
                DatabaseManager.closeResources(null, ps, conn);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("❌ ID inválido");
            if (out != null) {
                enviarError(out, "ID inválido");
            }
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            if (out != null) {
                enviarError(out, "Error del servidor");
            }
        } finally {
            if (out != null) {
                out.flush();
            }
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print("{\"success\": false, \"error\": \"Método no permitido\"}");
    }
    
    private void enviarError(PrintWriter out, String mensaje) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", mensaje);
        Gson gson = new Gson();
        out.print(gson.toJson(error));
    }
}