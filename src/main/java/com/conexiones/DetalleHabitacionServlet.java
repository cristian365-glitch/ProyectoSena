package com.conexiones;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/DetalleHabitacionServlet")
public class DetalleHabitacionServlet extends HttpServlet {
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
        
        String habitacionId = request.getParameter("id");
        
        if (habitacionId == null || habitacionId.isEmpty()) {
            response.getWriter().print("{\"error\": \"ID de habitación no proporcionado\"}");
            return;
        }
        
        JsonObject habitacion = null;
        
        try (Connection conn = dbManager.getConnection()) {
            
            // Obtener datos de la habitación
            String sqlHab = "SELECT id, codigo, nombre, experiencia, descripcion, precio_noche, capacidad, " +
                           "metros_cuadrados, wifi, aire_acondicionado, tv, minibar, balcon, vista, " +
                           "camas_info, desayuno_incluido, cancelacion_gratuita, imagen_principal " +
                           "FROM habitaciones WHERE id = ? AND activa = 1";
            
            try (PreparedStatement psHab = conn.prepareStatement(sqlHab)) {
                psHab.setInt(1, Integer.parseInt(habitacionId));
                
                try (ResultSet rs = psHab.executeQuery()) {
                    if (rs.next()) {
                        habitacion = new JsonObject();
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
                    }
                }
            }
            
            if (habitacion == null) {
                response.getWriter().print("{\"error\": \"Habitación no encontrada\"}");
                return;
            }
            
            // Obtener todas las imágenes de la habitación
            String sqlImg = "SELECT url_cloudinary, orden FROM imagenes_habitacion WHERE habitacion_id = ? ORDER BY orden ASC";
            JsonArray imagenes = new JsonArray();
            
            try (PreparedStatement psImg = conn.prepareStatement(sqlImg)) {
                psImg.setInt(1, Integer.parseInt(habitacionId));
                
                try (ResultSet rsImg = psImg.executeQuery()) {
                    while (rsImg.next()) {
                        JsonObject img = new JsonObject();
                        img.addProperty("url_cloudinary", rsImg.getString("url_cloudinary"));
                        img.addProperty("orden", rsImg.getInt("orden"));
                        imagenes.add(img);
                    }
                }
            }
            
            habitacion.add("imagenes", imagenes);
            
        } catch (SQLException e) {
            System.err.println("Error al obtener detalle de habitación: " + e.getMessage());
            e.printStackTrace();
            response.getWriter().print("{\"error\": \"Error del sistema\"}");
            return;
        } catch (NumberFormatException e) {
            response.getWriter().print("{\"error\": \"ID inválido\"}");
            return;
        }
        
        response.getWriter().print(habitacion.toString());
    }
}