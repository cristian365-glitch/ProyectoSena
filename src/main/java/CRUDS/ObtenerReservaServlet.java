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
     * ⭐ CORREGIDO: Obtiene todas las fechas ocupadas de una habitación específica
     * Ahora busca TODOS los estados que bloquean la habitación
     */
    private void obtenerFechasOcupadas(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      String habitacionIdStr) throws IOException {
        System.out.println("========================================");
        System.out.println("🔍 OBTENIENDO FECHAS OCUPADAS");
        System.out.println("   Habitación ID: " + habitacionIdStr);
        
        try {
            int habitacionId = Integer.parseInt(habitacionIdStr);
            
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = dbManager.getConnection();
                
                // ✅ CORREGIDO: Buscar TODAS las reservas que bloquean la habitación
                // Incluye: pendiente, confirmada, pendiente_verificacion, en_proceso
                // Excluye solo: cancelada, finalizada
                String sql = "SELECT id, fecha_checkin, fecha_checkout, estado " +
                           "FROM reservas " +
                           "WHERE habitacion_id = ? " +
                           "AND estado NOT IN ('cancelada', 'finalizada') " +
                           "AND fecha_checkout >= CURDATE() " +
                           "ORDER BY fecha_checkin";
                
                System.out.println("📋 Ejecutando query:");
                System.out.println("   SQL: " + sql);
                System.out.println("   Habitación ID: " + habitacionId);
                
                ps = conn.prepareStatement(sql);
                ps.setInt(1, habitacionId);
                rs = ps.executeQuery();
                
                List<String> fechasOcupadas = new ArrayList<>();
                int reservasEncontradas = 0;
                
                while (rs.next()) {
                    reservasEncontradas++;
                    int reservaId = rs.getInt("id");
                    String checkin = rs.getString("fecha_checkin");
                    String checkout = rs.getString("fecha_checkout");
                    String estado = rs.getString("estado");
                    
                    System.out.println("   📅 Reserva #" + reservaId + ":");
                    System.out.println("      Estado: " + estado);
                    System.out.println("      Check-in: " + checkin);
                    System.out.println("      Check-out: " + checkout);
                    
                    // Generar todas las fechas entre checkin y checkout
                    LocalDate inicio = LocalDate.parse(checkin);
                    LocalDate fin = LocalDate.parse(checkout);
                    
                    // ✅ IMPORTANTE: Incluir desde checkin hasta checkout-1
                    // El día de checkout NO se marca como ocupado (el cliente ya se fue)
                    LocalDate fecha = inicio;
                    while (fecha.isBefore(fin)) {
                        String fechaStr = fecha.toString();
                        if (!fechasOcupadas.contains(fechaStr)) {
                            fechasOcupadas.add(fechaStr);
                            System.out.println("      ✓ Bloqueando: " + fechaStr);
                        }
                        fecha = fecha.plusDays(1);
                    }
                }
                
                System.out.println("✅ Total de reservas encontradas: " + reservasEncontradas);
                System.out.println("✅ Total de fechas ocupadas: " + fechasOcupadas.size());
                System.out.println("📋 Fechas ocupadas: " + fechasOcupadas);
                System.out.println("========================================");
                
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("success", true);
                resultado.put("fechas_ocupadas", fechasOcupadas);
                resultado.put("total_reservas", reservasEncontradas);
                resultado.put("habitacion_id", habitacionId);
                
                PrintWriter out = response.getWriter();
                Gson gson = new Gson();
                out.print(gson.toJson(resultado));
                out.flush();
                
            } catch (SQLException e) {
                System.err.println("❌ Error SQL: " + e.getMessage());
                e.printStackTrace();
                enviarError(response, "Error al consultar fechas ocupadas");
            } finally {
                DatabaseManager.closeResources(rs, ps, conn);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("❌ ID de habitación inválido: " + habitacionIdStr);
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
            
            // ✅ CORREGIDO: Incluir todas las reservas activas
            String sql = "SELECT habitacion_id, fecha_checkin, fecha_checkout, estado " +
                        "FROM reservas " +
                        "WHERE estado NOT IN ('cancelada', 'finalizada')";
            
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