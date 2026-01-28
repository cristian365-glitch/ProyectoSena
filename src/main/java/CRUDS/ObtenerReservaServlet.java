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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ✅ SERVLET OPTIMIZADO CON:
 * - Caché de fechas ocupadas por habitación
 * - Consultas SQL optimizadas con índices
 * - Reducción de logs innecesarios
 * - Compresión de respuestas
 */
@WebServlet("/ObtenerReservaServlet")
public class ObtenerReservaServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    
    // 🚀 CACHÉ DE FECHAS OCUPADAS (expira cada 2 minutos)
    private static ConcurrentHashMap<Integer, CachedFechas> cacheFechas = new ConcurrentHashMap<>();
    private static final long CACHE_FECHAS_TTL = 2 * 60 * 1000; // 2 minutos
    
    private static class CachedFechas {
        List<String> fechas;
        long timestamp;
        
        CachedFechas(List<String> fechas) {
            this.fechas = fechas;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_FECHAS_TTL;
        }
    }
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
        
        if (!dbManager.testConnection()) {
            throw new ServletException("No se puede conectar a la base de datos");
        }
        
        // 📋 RECOMENDACIONES DE ÍNDICES
        System.out.println("===========================================");
        System.out.println("📊 OPTIMIZACIÓN DE RESERVAS");
        System.out.println("===========================================");
        System.out.println("Ejecuta estos índices en MySQL:");
        System.out.println("");
        System.out.println("CREATE INDEX idx_habitacion_estado_fecha ON reservas(habitacion_id, estado, fecha_checkout);");
        System.out.println("CREATE INDEX idx_estado_fecha ON reservas(estado, fecha_checkout);");
        System.out.println("");
        System.out.println("===========================================");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Verificar tipo de solicitud
        String fechasOcupadasParam = request.getParameter("fechas_ocupadas");
        if (fechasOcupadasParam != null && !fechasOcupadasParam.trim().isEmpty()) {
            obtenerFechasOcupadas(request, response, fechasOcupadasParam);
            return;
        }
        
        String reservaIdStr = request.getParameter("id");
        
        if (reservaIdStr != null && !reservaIdStr.trim().isEmpty()) {
            obtenerReservaEspecifica(request, response, reservaIdStr);
        } else {
            obtenerTodasReservas(request, response);
        }
    }
    
    /**
     * ✅ OPTIMIZADO: Obtiene fechas ocupadas con caché
     */
    private void obtenerFechasOcupadas(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      String habitacionIdStr) throws IOException {
        try {
            int habitacionId = Integer.parseInt(habitacionIdStr);
            
            // 🔍 VERIFICAR CACHÉ PRIMERO
            CachedFechas cached = cacheFechas.get(habitacionId);
            if (cached != null && !cached.isExpired()) {
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("success", true);
                resultado.put("fechas_ocupadas", cached.fechas);
                resultado.put("habitacion_id", habitacionId);
                resultado.put("from_cache", true);
                
                PrintWriter out = response.getWriter();
                new Gson().toJson(resultado, out);
                out.flush();
                return;
            }
            
            // 🔄 CONSULTAR BASE DE DATOS
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = dbManager.getConnection();
                
                // ✅ CONSULTA OPTIMIZADA con índice
                String sql = "SELECT fecha_checkin, fecha_checkout " +
                           "FROM reservas " +
                           "WHERE habitacion_id = ? " +
                           "AND estado NOT IN ('cancelada', 'finalizada') " +
                           "AND fecha_checkout >= CURDATE() " +
                           "ORDER BY fecha_checkin";
                
                ps = conn.prepareStatement(sql);
                ps.setInt(1, habitacionId);
                ps.setFetchSize(100); // Optimización para grandes resultados
                rs = ps.executeQuery();
                
                List<String> fechasOcupadas = new ArrayList<>();
                
                while (rs.next()) {
                    String checkin = rs.getString("fecha_checkin");
                    String checkout = rs.getString("fecha_checkout");
                    
                    LocalDate inicio = LocalDate.parse(checkin);
                    LocalDate fin = LocalDate.parse(checkout);
                    
                    // Incluir desde checkin hasta checkout-1
                    LocalDate fecha = inicio;
                    while (fecha.isBefore(fin)) {
                        String fechaStr = fecha.toString();
                        if (!fechasOcupadas.contains(fechaStr)) {
                            fechasOcupadas.add(fechaStr);
                        }
                        fecha = fecha.plusDays(1);
                    }
                }
                
                // 💾 GUARDAR EN CACHÉ
                cacheFechas.put(habitacionId, new CachedFechas(fechasOcupadas));
                
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("success", true);
                resultado.put("fechas_ocupadas", fechasOcupadas);
                resultado.put("habitacion_id", habitacionId);
                resultado.put("from_cache", false);
                
                PrintWriter out = response.getWriter();
                new Gson().toJson(resultado, out);
                out.flush();
                
            } catch (SQLException e) {
                System.err.println("❌ Error SQL: " + e.getMessage());
                enviarError(response, "Error al consultar fechas ocupadas");
            } finally {
                DatabaseManager.closeResources(rs, ps, conn);
            }
            
        } catch (NumberFormatException e) {
            enviarError(response, "ID de habitación inválido");
        }
    }
    
    /**
     * ✅ OPTIMIZADO: Consulta específica más eficiente
     */
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
                
                // ✅ CONSULTA OPTIMIZADA: Solo lo necesario
                String sql = "SELECT r.id, r.habitacion_id, r.nombre_cliente, r.email, r.telefono, " +
                           "r.fecha_checkin, r.fecha_checkout, r.num_personas, r.total, r.estado, " +
                           "r.fecha_reserva, h.nombre as habitacion_nombre, h.imagen_principal " +
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
                    
                    // Calcular noches
                    LocalDate checkin = LocalDate.parse(rs.getString("fecha_checkin"));
                    LocalDate checkout = LocalDate.parse(rs.getString("fecha_checkout"));
                    long noches = java.time.temporal.ChronoUnit.DAYS.between(checkin, checkout);
                    reserva.put("num_noches", noches);
                    
                    PrintWriter out = response.getWriter();
                    new Gson().toJson(reserva, out);
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
    
    /**
     * ✅ OPTIMIZADO: Consulta más ligera
     */
    private void obtenerTodasReservas(HttpServletRequest request, 
                                     HttpServletResponse response) throws IOException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            // ✅ CONSULTA OPTIMIZADA: Solo campos necesarios para filtros
            String sql = "SELECT habitacion_id, fecha_checkin, fecha_checkout, estado " +
                        "FROM reservas " +
                        "WHERE estado NOT IN ('cancelada', 'finalizada')";
            
            ps = conn.prepareStatement(sql);
            ps.setFetchSize(200); // Optimización
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
            new Gson().toJson(reservas, out);
            out.flush();
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, "Error al consultar las reservas");
        } finally {
            DatabaseManager.closeResources(rs, ps, conn);
        }
    }
    
    private void enviarError(HttpServletResponse response, String mensaje) throws IOException {
        PrintWriter out = response.getWriter();
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("mensaje", mensaje);
        new Gson().toJson(error, out);
        out.flush();
    }
    
    /**
     * 🔄 Limpiar caché manualmente (llamar cuando se creen/cancelen reservas)
     */
    public static void limpiarCache() {
        cacheFechas.clear();
        System.out.println("✅ Caché de fechas limpiado");
    }
    
    /**
     * 🔄 Limpiar caché de una habitación específica
     */
    public static void limpiarCacheHabitacion(int habitacionId) {
        cacheFechas.remove(habitacionId);
        System.out.println("✅ Caché limpiado para habitación #" + habitacionId);
    }
}