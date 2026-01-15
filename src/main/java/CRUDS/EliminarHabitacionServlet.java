package CRUDS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.conexiones.DatabaseManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/EliminarHabitacionServlet")
public class EliminarHabitacionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    
    @Override
protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    
    // Configurar respuesta JSON
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    // Verificar que sea admin
    HttpSession session = request.getSession(false);
    
    if (session == null || session.getAttribute("logueado") == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"success\": false, \"error\": \"No autenticado\"}");
        return;
    }
    
    Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
    
    if (esAdmin == null || !esAdmin) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write("{\"success\": false, \"error\": \"No autorizado\"}");
        return;
    }
    
    String idStr = request.getParameter("id");
    
    System.out.println("DEBUG - ID recibido: " + idStr); // Para debugging
    
    if (idStr == null || idStr.trim().isEmpty()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"success\": false, \"error\": \"ID no proporcionado\"}");
        return;
    }
    
    try {
        int id = Integer.parseInt(idStr.trim());
        
        Connection conn = null;
        PreparedStatement psDeleteImages = null;
        PreparedStatement psDeleteRoom = null;
        
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            
            // Primero eliminar imágenes asociadas
            String deleteImagesSql = "DELETE FROM imagenes_habitacion WHERE habitacion_id = ?";
            psDeleteImages = conn.prepareStatement(deleteImagesSql);
            psDeleteImages.setInt(1, id);
            int imagenes = psDeleteImages.executeUpdate();
            
            System.out.println("DEBUG - Imágenes eliminadas: " + imagenes);
            
            // Luego eliminar la habitación
            String deleteRoomSql = "DELETE FROM habitaciones WHERE id = ?";
            psDeleteRoom = conn.prepareStatement(deleteRoomSql);
            psDeleteRoom.setInt(1, id);
            
            int filas = psDeleteRoom.executeUpdate();
            
            System.out.println("DEBUG - Habitaciones eliminadas: " + filas);
            
            if (filas > 0) {
                conn.commit();
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"success\": true, \"message\": \"Habitación eliminada\"}");
            } else {
                conn.rollback();
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"success\": false, \"error\": \"Habitación no encontrada\"}");
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
            System.err.println("ERROR SQL: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"error\": \"Error de base de datos: " + e.getMessage() + "\"}");
        } finally {
            DatabaseManager.closeResources(null, psDeleteImages, psDeleteRoom, conn);
        }
        
    } catch (NumberFormatException e) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"success\": false, \"error\": \"ID inválido: " + idStr + "\"}");
    }
}
}