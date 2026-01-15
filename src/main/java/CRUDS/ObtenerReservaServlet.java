package CRUDS;

import com.conexiones.DatabaseManager;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet que maneja las operaciones relacionadas con reservas
 * 
 * ENDPOINTS:
 * - GET sin parámetros: Obtiene TODAS las reservas (para filtros)
 * - GET con ?id=X: Obtiene UNA reserva específica
 * - GET con ?fechas_ocupadas=habitacion_id: Obtiene fechas ocupadas de una habitación
 */
@WebServlet("/ObtenerReservaServlet")
public class ObtenerReservaServlet extends HttpServlet {
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // ⭐ NUEVO: Verificar si se solicitan fechas ocupadas
        String fechasOcupadasParam = request.getParameter("fechas_ocupadas");
        if (fechasOcupadasParam != null && !fechasOcupadasParam.trim().isEmpty()) {
            obtenerFechasOcupadas(request, response, fechasOcupadasParam);
            return;
        }
        
        // Verificar si se solicita una reserva específica
        String reservaIdStr = request.getParameter("id");
        
        if (reservaIdStr != null && !reservaIdStr.trim().isEmpty()) {
            obtenerReservaEspecifica(request, response, reservaIdStr);
        } else {
            obtenerTodasReservas(request, response);
        }
    }
    
    /**
     * ⭐ NUEVO: Obtiene todas las fechas ocupadas de una habitación específica
     * Se usa para marcar fechas en el calendario de reservas
     */
    private void obtenerFechasOcupadas(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      String habitacionIdStr) throws IOException {
        try {
            int habitacionId = Integer.parseInt(habitacionIdStr);
            
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = dbManager.getConnection();
                
                // Obtener reservas confirmadas o pendientes de verificación
                String sql = "SELECT fecha_checkin, fecha_checkout " +
                           "FROM reservas " +
                           "WHERE habitacion_id = ? " +
                           "AND estado IN ('confirmada', 'pendiente_verificacion') " +
                           "AND fecha_checkout >= CURDATE()";
                
                ps = conn.prepareStatement(sql);
                ps.setInt(1, habitacionId);
                rs = ps.executeQuery();
                
                List<String> fechasOcupadas = new ArrayList<>();
                
                while (rs.next()) {
                    String checkin = rs.getString("fecha_checkin");
                    String checkout = rs.getString("fecha_checkout");
                    
                    // Generar todas las fechas entre checkin y checkout
                    LocalDate inicio = LocalDate.parse(checkin);
                    LocalDate fin = LocalDate.parse(checkout);
                    
                    // Incluir desde checkin hasta checkout (no incluir la fecha de checkout)
                    LocalDate fecha = inicio;
                    while (!fecha.isAfter(fin.minusDays(1))) {
                        fechasOcupadas.add(fecha.toString());
                        fecha = fecha.plusDays(1);
                    }
                }
                
                System.out.println("✅ Fechas ocupadas habitación " + habitacionId + ": " + fechasOcupadas.size());
                
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("success", true);
                resultado.put("fechas_ocupadas", fechasOcupadas);
                
                PrintWriter out = response.getWriter();
                Gson gson = new Gson();
                out.print(gson.toJson(resultado));
                out.flush();
                
            } catch (SQLException e) {
                e.printStackTrace();
                enviarError(response, "Error al consultar fechas ocupadas");
            } finally {
                DatabaseManager.closeResources(rs, ps, conn);
            }
            
        } catch (NumberFormatException e) {
            enviarError(response, "ID de habitación inválido");
        }
    }
    
    private void obtenerReservaEspecifica(HttpServletRequest request, 
                                         HttpServletResponse response, 
                                         String reservaIdStr) throws IOException {
        try {
            int reservaId = Integer.parseInt(reservaIdStr);
            
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = dbManager.getConnection();
                
                String sql = "SELECT r.*, h.nombre as habitacion_nombre, h.imagen_principal " +
                           "FROM reservas r " +
                           "INNER JOIN habitaciones h ON r.habitacion_id = h.id " +
                           "WHERE r.id = ?";
                
                ps = conn.prepareStatement(sql);
                ps.setInt(1, reservaId);
                rs = ps.executeQuery();
                
                if (rs.next()) {
                    Map<String, Object> reserva = new HashMap<>();
                    
                    reserva.put("id", rs.getInt("id"));
                    reserva.put("habitacion_id", rs.getInt("habitacion_id"));
                    reserva.put("habitacion_nombre", rs.getString("habitacion_nombre"));
                    reserva.put("imagen_principal", rs.getString("imagen_principal"));
                    reserva.put("nombre_cliente", rs.getString("nombre_cliente"));
                    reserva.put("email", rs.getString("email"));
                    reserva.put("telefono", rs.getString("telefono"));
                    reserva.put("fecha_checkin", rs.getString("fecha_checkin"));
                    reserva.put("fecha_checkout", rs.getString("fecha_checkout"));
                    reserva.put("num_personas", rs.getInt("num_personas"));
                    reserva.put("total", rs.getDouble("total"));
                    reserva.put("estado", rs.getString("estado"));
                    reserva.put("fecha_reserva", rs.getString("fecha_reserva"));
                    
                    LocalDate checkin = LocalDate.parse(rs.getString("fecha_checkin"));
                    LocalDate checkout = LocalDate.parse(rs.getString("fecha_checkout"));
                    long noches = ChronoUnit.DAYS.between(checkin, checkout);
                    reserva.put("num_noches", noches);
                    
                    PrintWriter out = response.getWriter();
                    Gson gson = new Gson();
                    out.print(gson.toJson(reserva));
                    out.flush();
                } else {
                    enviarError(response, "Reserva no encontrada");
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                enviarError(response, "Error al consultar la base de datos");
            } finally {
                DatabaseManager.closeResources(rs, ps, conn);
            }
            
        } catch (NumberFormatException e) {
            enviarError(response, "ID de reserva inválido");
        }
    }
    
    private void obtenerTodasReservas(HttpServletRequest request, 
                                     HttpServletResponse response) throws IOException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            String sql = "SELECT habitacion_id, fecha_checkin, fecha_checkout, estado " +
                        "FROM reservas " +
                        "WHERE estado IN ('confirmada', 'pendiente')";
            
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            List<Map<String, Object>> reservas = new ArrayList<>();
            
            while (rs.next()) {
                Map<String, Object> reserva = new HashMap<>();
                reserva.put("habitacion_id", rs.getInt("habitacion_id"));
                reserva.put("fecha_checkin", rs.getString("fecha_checkin"));
                reserva.put("fecha_checkout", rs.getString("fecha_checkout"));
                reserva.put("estado", rs.getString("estado"));
                reservas.add(reserva);
            }
            
            PrintWriter out = response.getWriter();
            Gson gson = new Gson();
            out.print(gson.toJson(reservas));
            out.flush();
            
            System.out.println("✅ Enviadas " + reservas.size() + " reservas para filtros");
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, "Error al consultar las reservas");
        } finally {
            DatabaseManager.closeResources(rs, ps, conn);
        }
    }
    
    private void enviarError(HttpServletResponse response, String mensaje) throws IOException {
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("mensaje", mensaje);
        out.print(gson.toJson(error));
        out.flush();
    }
}