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
            response.sendRedirect(request.getContextPath() + "/Login.html?error=no_logueado&redirect=reservar");
            return;
        }
        
        // Obtener el ID del usuario de la sesión
        Integer userId = (Integer) session.getAttribute("userId");
        
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/Login.html?error=sesion_invalida");
            return;
        }
        
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        // Obtener datos del formulario
        String habitacionIdStr = request.getParameter("habitacion_id");
        String fechaCheckin = request.getParameter("fecha_checkin");
        String fechaCheckout = request.getParameter("fecha_checkout");
        String numPersonasStr = request.getParameter("num_personas");
        String nombreCliente = request.getParameter("nombre_cliente");
        String email = request.getParameter("email");
        String telefono = request.getParameter("telefono");
        String solicitudes = request.getParameter("solicitudes");
        
        // Validaciones básicas
        if (habitacionIdStr == null || habitacionIdStr.trim().isEmpty() ||
            fechaCheckin == null || fechaCheckin.trim().isEmpty() ||
            fechaCheckout == null || fechaCheckout.trim().isEmpty() ||
            numPersonasStr == null || numPersonasStr.trim().isEmpty() ||
            nombreCliente == null || nombreCliente.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            telefono == null || telefono.trim().isEmpty()) {
            
            response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
                                habitacionIdStr + "&error=campos_vacios");
            return;
        }
        
        try {
            int habitacionId = Integer.parseInt(habitacionIdStr);
            int numPersonas = Integer.parseInt(numPersonasStr);
            
            // Validar formato de fechas
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate checkin = LocalDate.parse(fechaCheckin, formatter);
            LocalDate checkout = LocalDate.parse(fechaCheckout, formatter);
            LocalDate hoy = LocalDate.now();
            
            // Validaciones de fechas
            if (checkin.isBefore(hoy)) {
                response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
                                    habitacionId + "&error=fecha_pasada");
                return;
            }
            
            if (checkout.isBefore(checkin) || checkout.isEqual(checkin)) {
                response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
                                    habitacionId + "&error=fechas_invalidas");
                return;
            }
            
            // Calcular número de noches y total
            long noches = ChronoUnit.DAYS.between(checkin, checkout);
            
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
                    response.sendRedirect(request.getContextPath() + "/index.html?error=habitacion_no_existe");
                    return;
                }
                
                double precioNoche = rsHabitacion.getDouble("precio_noche");
                int capacidad = rsHabitacion.getInt("capacidad");
                
                // Verificar capacidad
                if (numPersonas > capacidad) {
                    response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
                                        habitacionId + "&error=excede_capacidad");
                    return;
                }
                
                // Calcular total
                double total = precioNoche * noches;
                
                // Verificar disponibilidad de la habitación
                String sqlDisponibilidad = "SELECT COUNT(*) FROM reservas " +
                                          "WHERE habitacion_id = ? " +
                                          "AND estado != 'cancelada' " +
                                          "AND ((fecha_checkin BETWEEN ? AND ?) " +
                                          "OR (fecha_checkout BETWEEN ? AND ?) " +
                                          "OR (fecha_checkin <= ? AND fecha_checkout >= ?))";
                
                psDisponibilidad = conn.prepareStatement(sqlDisponibilidad);
                psDisponibilidad.setInt(1, habitacionId);
                psDisponibilidad.setString(2, fechaCheckin);
                psDisponibilidad.setString(3, fechaCheckout);
                psDisponibilidad.setString(4, fechaCheckin);
                psDisponibilidad.setString(5, fechaCheckout);
                psDisponibilidad.setString(6, fechaCheckin);
                psDisponibilidad.setString(7, fechaCheckout);
                
                rsDisponibilidad = psDisponibilidad.executeQuery();
                
                if (rsDisponibilidad.next() && rsDisponibilidad.getInt(1) > 0) {
                    response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
                                        habitacionId + "&error=no_disponible");
                    return;
                }
                
                // Insertar reserva
                String sqlInsert = "INSERT INTO reservas (habitacion_id, id_usuario, nombre_cliente, email, telefono, " +
                                  "fecha_checkin, fecha_checkout, num_personas, total, estado, fecha_reserva) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pendiente', NOW())";
                
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
                
                int filasAfectadas = psInsert.executeUpdate();
                
                if (filasAfectadas > 0) {
                    ResultSet rsId = psInsert.getGeneratedKeys();
                    int reservaId = 0;
                    if (rsId.next()) {
                        reservaId = rsId.getInt(1);
                    }
                    rsId.close();
                    
                    conn.commit();
                    
                    System.out.println("✅ Reserva creada exitosamente - ID: " + reservaId + " - Usuario: " + userId);
                    
                    // 🎯 REDIRIGIR A PÁGINA DE CONFIRMACIÓN Y PAGO
                    response.sendRedirect(request.getContextPath() + "/confirmacion-reserva.html?id=" + reservaId);
                } else {
                    conn.rollback();
                    response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
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
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
                                    habitacionId + "&error=error_sistema");
            } finally {
                DatabaseManager.closeResources(rsHabitacion, psHabitacion, null);
                DatabaseManager.closeResources(rsDisponibilidad, psDisponibilidad, null);
                DatabaseManager.closeResources(null, psInsert, conn);
            }
            
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
                                habitacionIdStr + "&error=datos_invalidos");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/reservar.html?habitacion=" + 
                                habitacionIdStr + "&error=error_sistema");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/index.html");
    }
}