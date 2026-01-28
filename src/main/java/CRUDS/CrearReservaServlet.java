package CRUDS;

import com.conexiones.DatabaseManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/CrearReservaServlet")
public class CrearReservaServlet extends HttpServlet {
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
        
        // Verificar que el usuario esté logueado
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("logueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/Login.html?error=no_logueado&redirect=reservar");
            return;
        }
        
        // Obtener el ID del usuario de la sesión
        Integer userId = (Integer) session.getAttribute("userId");
        
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/login/Login.html?error=sesion_invalida");
            return;
        }
        
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        // Obtener datos del formulario
        String habitacionIdStr = request.getParameter("habitacion_id");
        String numPersonasStr = request.getParameter("num_personas");
        String nombreCliente = request.getParameter("nombre_cliente");
        String email = request.getParameter("email");
        String telefono = request.getParameter("telefono");
        String solicitudes = request.getParameter("solicitudes");
        String fechaCheckin = request.getParameter("fecha_checkin");
        String fechaCheckout = request.getParameter("fecha_checkout");
        
        System.out.println("========================================");
        System.out.println("🔥 CREAR RESERVA - Datos recibidos:");
        System.out.println("   Habitación ID: " + habitacionIdStr);
        System.out.println("   Check-in: " + fechaCheckin);
        System.out.println("   Check-out: " + fechaCheckout);
        System.out.println("   Usuario ID: " + userId);
        System.out.println("   Personas: " + numPersonasStr);
        System.out.println("========================================");
        
        // Validaciones básicas
        if (habitacionIdStr == null || habitacionIdStr.trim().isEmpty() ||
            fechaCheckin == null || fechaCheckin.trim().isEmpty() ||
            fechaCheckout == null || fechaCheckout.trim().isEmpty() ||
            numPersonasStr == null || numPersonasStr.trim().isEmpty() ||
            nombreCliente == null || nombreCliente.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            telefono == null || telefono.trim().isEmpty()) {
            
            System.err.println("❌ Error: Campos vacíos");
            response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                habitacionIdStr + "&error=campos_vacios");
            return;
        }
        
        try {
            int habitacionId = Integer.parseInt(habitacionIdStr);
            int numPersonas = Integer.parseInt(numPersonasStr);
            
            // Validar formato de fechas (debe ser yyyy-MM-dd)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate checkin = LocalDate.parse(fechaCheckin, formatter);
            LocalDate checkout = LocalDate.parse(fechaCheckout, formatter);
            LocalDate hoy = LocalDate.now();
            
            // Validaciones de fechas
            if (checkin.isBefore(hoy)) {
                System.err.println("❌ Error: Fecha de check-in en el pasado");
                response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                    habitacionId + "&error=fecha_pasada");
                return;
            }
            
            if (checkout.isBefore(checkin) || checkout.isEqual(checkin)) {
                System.err.println("❌ Error: Fechas inválidas");
                response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                    habitacionId + "&error=fechas_invalidas");
                return;
            }
            
            // Calcular número de noches y total
            long noches = ChronoUnit.DAYS.between(checkin, checkout);
            System.out.println("📊 Número de noches: " + noches);
            
            Connection conn = null;
            PreparedStatement psHabitacion = null;
            PreparedStatement psDisponibilidad = null;
            PreparedStatement psInsert = null;
            ResultSet rsHabitacion = null;
            ResultSet rsDisponibilidad = null;
            
            try {
                conn = dbManager.getConnection();
                conn.setAutoCommit(false);
                
                // Obtener precio de la habitación y verificar capacidad
                String sqlHabitacion = "SELECT precio_noche, capacidad FROM habitaciones WHERE id = ?";
                psHabitacion = conn.prepareStatement(sqlHabitacion);
                psHabitacion.setInt(1, habitacionId);
                rsHabitacion = psHabitacion.executeQuery();
                
                if (!rsHabitacion.next()) {
                    System.err.println("❌ Error: Habitación no existe");
                    response.sendRedirect(request.getContextPath() + "/index.html?error=habitacion_no_existe");
                    return;
                }
                
                double precioNoche = rsHabitacion.getDouble("precio_noche");
                int capacidad = rsHabitacion.getInt("capacidad");
                
                System.out.println("💰 Precio por noche: $" + precioNoche);
                System.out.println("👥 Capacidad: " + capacidad);
                
                // Verificar capacidad
                if (numPersonas > capacidad) {
                    System.err.println("❌ Error: Excede capacidad (" + numPersonas + " > " + capacidad + ")");
                    response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                        habitacionId + "&error=excede_capacidad");
                    return;
                }
                
                // Calcular total
                double total = precioNoche * noches;
                System.out.println("💵 Total a pagar: $" + total);
                
                // ⭐ VERIFICACIÓN DE DISPONIBILIDAD MEJORADA
                System.out.println("🔍 Verificando disponibilidad...");
                System.out.println("   Rango solicitado: " + fechaCheckin + " → " + fechaCheckout);
                
                // ✅ QUERY MEJORADA - Detecta CUALQUIER solapamiento
                // Excluye solo reservas canceladas y finalizadas
                String sqlDisponibilidad = 
                    "SELECT id, fecha_checkin, fecha_checkout, estado " +
                    "FROM reservas " +
                    "WHERE habitacion_id = ? " +
                    "AND estado NOT IN ('cancelada', 'finalizada') " +
                    "AND NOT (" +
                    "  fecha_checkout <= ? OR " +  // Termina antes o el mismo día del checkin
                    "  fecha_checkin >= ? " +       // Empieza el día del checkout o después
                    ")";
                
                psDisponibilidad = conn.prepareStatement(sqlDisponibilidad);
                psDisponibilidad.setInt(1, habitacionId);
                psDisponibilidad.setString(2, fechaCheckin);
                psDisponibilidad.setString(3, fechaCheckout);
                
                rsDisponibilidad = psDisponibilidad.executeQuery();
                
                if (rsDisponibilidad.next()) {
                    // Hay conflicto de fechas
                    System.err.println("❌ CONFLICTO DE FECHAS DETECTADO:");
                    do {
                        System.err.println("   Reserva ID: " + rsDisponibilidad.getInt("id"));
                        System.err.println("   Ocupada desde: " + rsDisponibilidad.getString("fecha_checkin"));
                        System.err.println("   Hasta: " + rsDisponibilidad.getString("fecha_checkout"));
                        System.err.println("   Estado: " + rsDisponibilidad.getString("estado"));
                    } while (rsDisponibilidad.next());
                    
                    response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                        habitacionId + "&error=no_disponible&checkin=" + fechaCheckin + "&checkout=" + fechaCheckout);
                    return;
                }
                
                System.out.println("✅ Habitación disponible para las fechas solicitadas");
                
                // Insertar reserva
                String sqlInsert = "INSERT INTO reservas (habitacion_id, id_usuario, nombre_cliente, email, telefono, " +
                                  "fecha_checkin, fecha_checkout, num_personas, total, estado, fecha_reserva" +
                                  (solicitudes != null && !solicitudes.trim().isEmpty() ? ", solicitudes" : "") +
                                  ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pendiente', NOW()" +
                                  (solicitudes != null && !solicitudes.trim().isEmpty() ? ", ?" : "") +
                                  ")";
                
                psInsert = conn.prepareStatement(sqlInsert, PreparedStatement.RETURN_GENERATED_KEYS);
                psInsert.setInt(1, habitacionId);
                psInsert.setInt(2, userId);
                psInsert.setString(3, nombreCliente.trim());
                psInsert.setString(4, email.trim().toLowerCase());
                psInsert.setString(5, telefono.trim());
                psInsert.setString(6, fechaCheckin);
                psInsert.setString(7, fechaCheckout);
                psInsert.setInt(8, numPersonas);
                psInsert.setDouble(9, total);
                
                if (solicitudes != null && !solicitudes.trim().isEmpty()) {
                    psInsert.setString(10, solicitudes.trim());
                }
                
                int filasAfectadas = psInsert.executeUpdate();
                
                if (filasAfectadas > 0) {
                    ResultSet rsId = psInsert.getGeneratedKeys();
                    int reservaId = 0;
                    if (rsId.next()) {
                        reservaId = rsId.getInt(1);
                    }
                    rsId.close();
                    
                    conn.commit();
                    
                    System.out.println("✅ RESERVA CREADA EXITOSAMENTE!");
                    System.out.println("   Reserva ID: " + reservaId);
                    System.out.println("   Usuario ID: " + userId);
                    System.out.println("   Total: $" + total);
                    System.out.println("   Check-in: " + fechaCheckin);
                    System.out.println("   Check-out: " + fechaCheckout);
                    System.out.println("========================================");
                    
                    response.sendRedirect(request.getContextPath() + "/login/confirmacion-reserva.html?id=" + reservaId);
                } else {
                    conn.rollback();
                    System.err.println("❌ Error: No se pudo guardar la reserva");
                    response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                        habitacionId + "&error=no_guardado");
                }
                
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                System.err.println("❌ Error SQL: " + e.getMessage());
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                    habitacionId + "&error=error_sistema");
            } finally {
                DatabaseManager.closeResources(rsHabitacion, psHabitacion, null);
                DatabaseManager.closeResources(rsDisponibilidad, psDisponibilidad, null);
                DatabaseManager.closeResources(null, psInsert, conn);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Error: Datos inválidos - " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                habitacionIdStr + "&error=datos_invalidos");
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/login/reservar.html?habitacion=" + 
                                habitacionIdStr + "&error=error_sistema");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/index.html");
    }
}