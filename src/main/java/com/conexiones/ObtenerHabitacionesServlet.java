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

@WebServlet("/ObtenerHabitacionesServlet")
public class ObtenerHabitacionesServlet extends HttpServlet {
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
        
        String experiencia = request.getParameter("experiencia");
        
        JsonArray habitaciones = new JsonArray();
        
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT id, codigo, nombre, experiencia, descripcion, precio_noche, capacidad, " +
                        "metros_cuadrados, wifi, imagen_principal " +
                        "FROM habitaciones WHERE activa = 1";
            
            // Si se pasa un filtro de experiencia
            if (experiencia != null && !experiencia.isEmpty()) {
                sql += " AND experiencia = ?";
            }
            
            sql += " ORDER BY experiencia, nombre";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                
                if (experiencia != null && !experiencia.isEmpty()) {
                    ps.setString(1, experiencia);
                }
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        JsonObject hab = new JsonObject();
                        hab.addProperty("id", rs.getInt("id"));
                        hab.addProperty("codigo", rs.getString("codigo"));
                        hab.addProperty("nombre", rs.getString("nombre"));
                        hab.addProperty("experiencia", rs.getString("experiencia"));
                        hab.addProperty("descripcion", rs.getString("descripcion"));
                        hab.addProperty("precio_noche", rs.getDouble("precio_noche"));
                        hab.addProperty("capacidad", rs.getInt("capacidad"));
                        hab.addProperty("metros_cuadrados", rs.getInt("metros_cuadrados"));
                        hab.addProperty("wifi", rs.getBoolean("wifi"));
                        hab.addProperty("imagen_principal", rs.getString("imagen_principal"));
                        
                        habitaciones.add(hab);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener habitaciones: " + e.getMessage());
            e.printStackTrace();
        }
        
        response.getWriter().print(habitaciones.toString());
    }
}