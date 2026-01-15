package com.pagos;

import com.conexiones.DatabaseManager;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/ActualizarPagoMPServlet")
public class ActualizarPagoMPServlet extends HttpServlet {
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
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String reservaIdStr = request.getParameter("reserva_id");
        String paymentId = request.getParameter("payment_id");
        String status = request.getParameter("status");
        
        System.out.println("========================================");
        System.out.println("💳 Actualizando pago desde página de retorno");
        System.out.println("   Reserva ID: " + reservaIdStr);
        System.out.println("   Payment ID: " + paymentId);
        System.out.println("   Status: " + status);
        System.out.println("========================================");
        
        if (reservaIdStr == null || paymentId == null) {
            enviarError(out, "Parámetros faltantes");
            return;
        }
        
        try {
            int reservaId = Integer.parseInt(reservaIdStr);
            
            Connection conn = null;
            PreparedStatement ps = null;
            
            try {
                conn = dbManager.getConnection();
                
                // Determinar estado según el status de MP
                String nuevoEstado = "confirmada"; // Por defecto
                
                if ("approved".equals(status)) {
                    nuevoEstado = "confirmada";
                } else if ("pending".equals(status) || "in_process".equals(status)) {
                    nuevoEstado = "pendiente_pago";
                } else if ("rejected".equals(status) || "cancelled".equals(status)) {
                    nuevoEstado = "cancelada";
                }
                
                String sql = "UPDATE reservas SET " +
                           "estado = ?, " +
                           "mercadopago_payment_id = ?, " +
                           "mercadopago_payment_status = ? " +
                           "WHERE id = ?";
                
                ps = conn.prepareStatement(sql);
                ps.setString(1, nuevoEstado);
                ps.setString(2, paymentId);
                ps.setString(3, status);
                ps.setInt(4, reservaId);
                
                int filasActualizadas = ps.executeUpdate();
                
                if (filasActualizadas > 0) {
                    System.out.println("✅ Reserva actualizada correctamente");
                    System.out.println("   Nuevo estado: " + nuevoEstado);
                    System.out.println("========================================");
                    
                    Map<String, Object> respuesta = new HashMap<>();
                    respuesta.put("success", true);
                    respuesta.put("estado", nuevoEstado);
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
            enviarError(out, "ID inválido");
        }
        
        out.flush();
        out.close();
    }
    
    private void enviarError(PrintWriter out, String mensaje) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", mensaje);
        Gson gson = new Gson();
        out.print(gson.toJson(error));
    }
}