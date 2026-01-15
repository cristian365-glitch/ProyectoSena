package CRUDS;

import com.conexiones.DatabaseManager;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/ListarReservasServlet")
public class ListarReservasServlet extends HttpServlet {
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
        
        // Verificar que sea admin
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("logueado") == null) {
            enviarError(response, "No autenticado");
            return;
        }
        
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        
        if (esAdmin == null || !esAdmin) {
            enviarError(response, "No autorizado");
            return;
        }
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            // Consultar todas las reservas con información de habitación
            String sql = "SELECT r.*, h.nombre as habitacion_nombre " +
                       "FROM reservas r " +
                       "LEFT JOIN habitaciones h ON r.habitacion_id = h.id " +
                       "ORDER BY r.fecha_reserva DESC";
            
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            List<Map<String, Object>> reservas = new ArrayList<>();
            
            while (rs.next()) {
                Map<String, Object> reserva = new HashMap<>();
                
                reserva.put("id", rs.getInt("id"));
                reserva.put("habitacion_id", rs.getInt("habitacion_id"));
                reserva.put("habitacion_nombre", rs.getString("habitacion_nombre"));
                reserva.put("nombre_cliente", rs.getString("nombre_cliente"));
                reserva.put("email", rs.getString("email"));
                reserva.put("telefono", rs.getString("telefono"));
                reserva.put("fecha_checkin", rs.getString("fecha_checkin"));
                reserva.put("fecha_checkout", rs.getString("fecha_checkout"));
                reserva.put("num_personas", rs.getInt("num_personas"));
                reserva.put("total", rs.getDouble("total"));
                reserva.put("estado", rs.getString("estado"));
                reserva.put("fecha_reserva", rs.getString("fecha_reserva"));
                
                reservas.add(reserva);
            }
            
            PrintWriter out = response.getWriter();
            Gson gson = new Gson();
            out.print(gson.toJson(reservas));
            out.flush();
            
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(response, "Error al consultar la base de datos");
        } finally {
            DatabaseManager.closeResources(rs, ps, conn);
        }
    }
    
    private void enviarError(HttpServletResponse response, String mensaje) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("mensaje", mensaje);
        out.print(gson.toJson(error));
        out.flush();
    }
}