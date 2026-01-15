package UX;

import com.conexiones.DatabaseManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/ReservasServlet")
public class ReservasServlet extends HttpServlet {
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
            System.out.println("✅ ReservasServlet inicializado correctamente");
        } catch (Exception e) {
            System.err.println("❌ Error al inicializar ReservasServlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Error al inicializar servlet", e);
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        System.out.println("📥 Petición recibida en ReservasServlet");
        
        try {
            // Verificar que el usuario esté logueado
            HttpSession session = request.getSession(false);
            
            if (session == null || session.getAttribute("logueado") == null) {
                System.out.println("❌ No hay sesión activa");
                enviarError(response, out, "No hay sesión activa", 401);
                return;
            }
            
            // Obtener el ID del usuario de la sesión
            Integer userId = (Integer) session.getAttribute("userId");
            Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
            
            System.out.println("👤 Usuario ID: " + userId + " (Admin: " + esAdmin + ")");
            
            if (userId == null) {
                System.out.println("❌ userId es null");
                enviarError(response, out, "ID de usuario no encontrado", 400);
                return;
            }
            
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = dbManager.getConnection();
                System.out.println("✅ Conexión obtenida");
                
                String sql;
                
                // Si es admin, puede ver todas las reservas
                if (esAdmin != null && esAdmin) {
                    System.out.println("🔍 Obteniendo TODAS las reservas (Admin)");
                    sql = "SELECT r.id, r.habitacion_id, r.id_usuario, r.nombre_cliente, r.email, r.telefono, " +
                          "r.fecha_checkin, r.fecha_checkout, r.num_personas, r.total, r.estado, r.fecha_reserva, " +
                          "h.codigo, h.nombre, h.experiencia, h.precio_noche, h.imagen_principal " +
                          "FROM reservas r " +
                          "INNER JOIN habitaciones h ON r.habitacion_id = h.id " +
                          "ORDER BY r.fecha_reserva DESC";
                    ps = conn.prepareStatement(sql);
                } else {
                    System.out.println("🔍 Obteniendo reservas del usuario: " + userId);
                    sql = "SELECT r.id, r.habitacion_id, r.id_usuario, r.nombre_cliente, r.email, r.telefono, " +
                          "r.fecha_checkin, r.fecha_checkout, r.num_personas, r.total, r.estado, r.fecha_reserva, " +
                          "h.codigo, h.nombre, h.experiencia, h.precio_noche, h.imagen_principal " +
                          "FROM reservas r " +
                          "INNER JOIN habitaciones h ON r.habitacion_id = h.id " +
                          "WHERE r.id_usuario = ? " +
                          "ORDER BY r.fecha_reserva DESC";
                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, userId);
                }
                
                rs = ps.executeQuery();
                System.out.println("✅ Query ejecutado");
                
                // Construir respuesta JSON con librería
                JSONArray reservas = new JSONArray();
                int count = 0;
                
                while (rs.next()) {
                    JSONObject reserva = new JSONObject();
                    
                    // Datos de la reserva
                    reserva.put("id", rs.getInt("id"));
                    reserva.put("habitacion_id", rs.getInt("habitacion_id"));
                    reserva.put("id_usuario", rs.getInt("id_usuario"));
                    reserva.put("nombre_cliente", rs.getString("nombre_cliente"));
                    reserva.put("email", rs.getString("email"));
                    reserva.put("telefono", rs.getString("telefono"));
                    reserva.put("fecha_checkin", rs.getString("fecha_checkin"));
                    reserva.put("fecha_checkout", rs.getString("fecha_checkout"));
                    reserva.put("num_personas", rs.getInt("num_personas"));
                    reserva.put("total", rs.getDouble("total"));
                    reserva.put("estado", rs.getString("estado"));
                    reserva.put("fecha_reserva", rs.getString("fecha_reserva"));
                    
                    // Datos de la habitación
                    reserva.put("habitacion_codigo", rs.getString("codigo"));
                    reserva.put("habitacion_nombre", rs.getString("nombre"));
                    reserva.put("habitacion_experiencia", rs.getString("experiencia"));
                    reserva.put("precio_noche", rs.getDouble("precio_noche"));
                    reserva.put("imagen_principal", rs.getString("imagen_principal"));
                    
                    reservas.put(reserva);
                    count++;
                }
                
                System.out.println("✅ Reservas obtenidas: " + count);
                
                JSONObject resultado = new JSONObject();
                resultado.put("success", true);
                resultado.put("reservas", reservas);
                resultado.put("total", count);
                
                out.print(resultado.toString());
                out.flush();
                
            } catch (SQLException e) {
                System.err.println("❌ Error SQL: " + e.getMessage());
                e.printStackTrace();
                enviarError(response, out, "Error al obtener las reservas: " + e.getMessage(), 500);
                
            } finally {
                DatabaseManager.closeResources(rs, ps, conn);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
            e.printStackTrace();
            enviarError(response, out, "Error del servidor: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Obtener una reserva específica por ID
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            HttpSession session = request.getSession(false);
            
            if (session == null || session.getAttribute("logueado") == null) {
                enviarError(response, out, "No hay sesión activa", 401);
                return;
            }
            
            Integer userId = (Integer) session.getAttribute("userId");
            Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
            
            String reservaIdStr = request.getParameter("reserva_id");
            
            if (reservaIdStr == null || reservaIdStr.trim().isEmpty()) {
                enviarError(response, out, "ID de reserva no proporcionado", 400);
                return;
            }
            
            int reservaId = Integer.parseInt(reservaIdStr);
            
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = dbManager.getConnection();
                
                String sql;
                
                if (esAdmin != null && esAdmin) {
                    sql = "SELECT r.*, h.codigo, h.nombre, h.experiencia, h.descripcion, " +
                          "h.precio_noche, h.imagen_principal, h.capacidad " +
                          "FROM reservas r " +
                          "INNER JOIN habitaciones h ON r.habitacion_id = h.id " +
                          "WHERE r.id = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, reservaId);
                } else {
                    sql = "SELECT r.*, h.codigo, h.nombre, h.experiencia, h.descripcion, " +
                          "h.precio_noche, h.imagen_principal, h.capacidad " +
                          "FROM reservas r " +
                          "INNER JOIN habitaciones h ON r.habitacion_id = h.id " +
                          "WHERE r.id = ? AND r.id_usuario = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, reservaId);
                    ps.setInt(2, userId);
                }
                
                rs = ps.executeQuery();
                
                if (rs.next()) {
                    JSONObject reserva = new JSONObject();
                    
                    reserva.put("id", rs.getInt("id"));
                    reserva.put("habitacion_id", rs.getInt("habitacion_id"));
                    reserva.put("id_usuario", rs.getInt("id_usuario"));
                    reserva.put("nombre_cliente", rs.getString("nombre_cliente"));
                    reserva.put("email", rs.getString("email"));
                    reserva.put("telefono", rs.getString("telefono"));
                    reserva.put("fecha_checkin", rs.getString("fecha_checkin"));
                    reserva.put("fecha_checkout", rs.getString("fecha_checkout"));
                    reserva.put("num_personas", rs.getInt("num_personas"));
                    reserva.put("total", rs.getDouble("total"));
                    reserva.put("estado", rs.getString("estado"));
                    reserva.put("fecha_reserva", rs.getString("fecha_reserva"));
                    
                    reserva.put("habitacion_codigo", rs.getString("codigo"));
                    reserva.put("habitacion_nombre", rs.getString("nombre"));
                    reserva.put("habitacion_experiencia", rs.getString("experiencia"));
                    reserva.put("habitacion_descripcion", rs.getString("descripcion"));
                    reserva.put("habitacion_capacidad", rs.getInt("capacidad"));
                    reserva.put("precio_noche", rs.getDouble("precio_noche"));
                    reserva.put("imagen_principal", rs.getString("imagen_principal"));
                    
                    JSONObject resultado = new JSONObject();
                    resultado.put("success", true);
                    resultado.put("reserva", reserva);
                    
                    out.print(resultado.toString());
                    out.flush();
                    
                } else {
                    enviarError(response, out, "Reserva no encontrada o no tienes permiso para verla", 404);
                }
                
            } finally {
                DatabaseManager.closeResources(rs, ps, conn);
            }
            
        } catch (NumberFormatException e) {
            enviarError(response, out, "ID de reserva inválido", 400);
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, out, "Error al obtener la reserva", 500);
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
}