package CRUDS;

import com.conexiones.DatabaseManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/ObtenerHabitacionServletCRUD")
public class ObtenerHabitacionServletCRUD extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
        gson = new Gson();
        
        if (!dbManager.testConnection()) {
            throw new ServletException("No se puede conectar a la base de datos");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String idParam = request.getParameter("id");
        
        // Si se proporciona ID, obtener una habitación específica
        if (idParam != null && !idParam.trim().isEmpty()) {
            obtenerHabitacionPorId(request, response, idParam);
        } else {
            // Si no hay ID, obtener todas las habitaciones
            obtenerTodasLasHabitaciones(request, response);
        }
    }
    
    private void obtenerHabitacionPorId(HttpServletRequest request, HttpServletResponse response, String idParam)
            throws IOException {
        
        Connection conn = null;
        PreparedStatement psHabitacion = null;
        PreparedStatement psImagenes = null;
        ResultSet rsHabitacion = null;
        ResultSet rsImagenes = null;
        
        try {
            int id = Integer.parseInt(idParam);
            conn = dbManager.getConnection();
            
            // Obtener datos de la habitación
            String sqlHabitacion = "SELECT * FROM habitaciones WHERE id = ?";
            psHabitacion = conn.prepareStatement(sqlHabitacion);
            psHabitacion.setInt(1, id);
            rsHabitacion = psHabitacion.executeQuery();
            
            if (rsHabitacion.next()) {
                JsonObject habitacion = new JsonObject();
                
                habitacion.addProperty("id", rsHabitacion.getInt("id"));
                habitacion.addProperty("codigo", rsHabitacion.getString("codigo"));
                habitacion.addProperty("nombre", rsHabitacion.getString("nombre"));
                habitacion.addProperty("experiencia", rsHabitacion.getString("experiencia"));
                habitacion.addProperty("descripcion", rsHabitacion.getString("descripcion"));
                habitacion.addProperty("precio_noche", rsHabitacion.getDouble("precio_noche"));
                habitacion.addProperty("capacidad", rsHabitacion.getInt("capacidad"));
                habitacion.addProperty("metros_cuadrados", rsHabitacion.getInt("metros_cuadrados"));
                habitacion.addProperty("wifi", rsHabitacion.getBoolean("wifi"));
                habitacion.addProperty("aire_acondicionado", rsHabitacion.getBoolean("aire_acondicionado"));
                habitacion.addProperty("tv", rsHabitacion.getBoolean("tv"));
                habitacion.addProperty("minibar", rsHabitacion.getBoolean("minibar"));
                habitacion.addProperty("balcon", rsHabitacion.getBoolean("balcon"));
                habitacion.addProperty("vista", rsHabitacion.getString("vista"));
                habitacion.addProperty("camas_info", rsHabitacion.getString("camas_info"));
                habitacion.addProperty("desayuno_incluido", rsHabitacion.getBoolean("desayuno_incluido"));
                habitacion.addProperty("cancelacion_gratuita", rsHabitacion.getBoolean("cancelacion_gratuita"));
                habitacion.addProperty("imagen_principal", rsHabitacion.getString("imagen_principal"));
                
                // Obtener imágenes
                String sqlImagenes = "SELECT * FROM imagenes_habitacion WHERE habitacion_id = ? ORDER BY orden";
                psImagenes = conn.prepareStatement(sqlImagenes);
                psImagenes.setInt(1, id);
                rsImagenes = psImagenes.executeQuery();
                
                JsonArray imagenes = new JsonArray();
                while (rsImagenes.next()) {
                    JsonObject imagen = new JsonObject();
                    imagen.addProperty("id", rsImagenes.getInt("id"));
                    imagen.addProperty("url_cloudinary", rsImagenes.getString("url_cloudinary"));
                    imagen.addProperty("orden", rsImagenes.getInt("orden"));
                    imagen.addProperty("es_principal", rsImagenes.getBoolean("es_principal"));
                    imagenes.add(imagen);
                }
                
                habitacion.add("imagenes", imagenes);
                
                response.getWriter().write(gson.toJson(habitacion));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Habitación no encontrada");
                response.getWriter().write(gson.toJson(error));
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject error = new JsonObject();
            error.addProperty("error", "ID inválido");
            response.getWriter().write(gson.toJson(error));
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error del sistema");
            response.getWriter().write(gson.toJson(error));
        } finally {
            DatabaseManager.closeResources(rsHabitacion, rsImagenes, psHabitacion, psImagenes, conn);
        }
    }
    
    private void obtenerTodasLasHabitaciones(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            String sql = "SELECT * FROM habitaciones ORDER BY experiencia, nombre";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            JsonArray habitaciones = new JsonArray();
            
            while (rs.next()) {
                JsonObject habitacion = new JsonObject();
                
                habitacion.addProperty("id", rs.getInt("id"));
                habitacion.addProperty("codigo", rs.getString("codigo"));
                habitacion.addProperty("nombre", rs.getString("nombre"));
                habitacion.addProperty("experiencia", rs.getString("experiencia"));
                habitacion.addProperty("descripcion", rs.getString("descripcion"));
                habitacion.addProperty("precio_noche", rs.getDouble("precio_noche"));
                habitacion.addProperty("capacidad", rs.getInt("capacidad"));
                habitacion.addProperty("metros_cuadrados", rs.getInt("metros_cuadrados"));
                habitacion.addProperty("wifi", rs.getBoolean("wifi"));
                habitacion.addProperty("aire_acondicionado", rs.getBoolean("aire_acondicionado"));
                habitacion.addProperty("tv", rs.getBoolean("tv"));
                habitacion.addProperty("minibar", rs.getBoolean("minibar"));
                habitacion.addProperty("balcon", rs.getBoolean("balcon"));
                habitacion.addProperty("vista", rs.getString("vista"));
                habitacion.addProperty("camas_info", rs.getString("camas_info"));
                habitacion.addProperty("desayuno_incluido", rs.getBoolean("desayuno_incluido"));
                habitacion.addProperty("cancelacion_gratuita", rs.getBoolean("cancelacion_gratuita"));
                habitacion.addProperty("imagen_principal", rs.getString("imagen_principal"));
                
                habitaciones.add(habitacion);
            }
            
            response.getWriter().write(gson.toJson(habitaciones));
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error al obtener habitaciones");
            response.getWriter().write(gson.toJson(error));
        } finally {
            DatabaseManager.closeResources(rs, ps, conn);
        }
    }
}