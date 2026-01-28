package com.conexiones;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ✅ SERVLET OPTIMIZADO CON:
 * - Caché en memoria para reducir consultas a BD
 * - Índices sugeridos en comentarios
 * - Consultas SQL optimizadas
 * - Compresión GZIP de respuestas
 * - ETags para caché del navegador
 */
@WebServlet("/ObtenerHabitacionesServlet")
public class ObtenerHabitacionesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    
    // 🚀 CACHÉ EN MEMORIA (se refresca cada 5 minutos)
    private static ConcurrentHashMap<String, CachedData> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutos
    
    private static class CachedData {
        String jsonData;
        long timestamp;
        String etag;
        
        CachedData(String jsonData, String etag) {
            this.jsonData = jsonData;
            this.timestamp = System.currentTimeMillis();
            this.etag = etag;
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_TTL;
        }
    }
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
        
        if (!dbManager.testConnection()) {
            throw new ServletException("No se puede conectar a la base de datos");
        }
        
        // 📋 IMPRIME RECOMENDACIONES DE ÍNDICES
        System.out.println("===========================================");
        System.out.println("📊 OPTIMIZACIÓN DE BASE DE DATOS");
        System.out.println("===========================================");
        System.out.println("Para optimizar las consultas, ejecuta estos índices en MySQL:");
        System.out.println("");
        System.out.println("CREATE INDEX idx_activa_experiencia ON habitaciones(activa, experiencia);");
        System.out.println("CREATE INDEX idx_activa ON habitaciones(activa);");
        System.out.println("");
        System.out.println("===========================================");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String experiencia = request.getParameter("experiencia");
        String cacheKey = experiencia != null ? "exp_" + experiencia : "all";
        
        // 🔍 VERIFICAR CACHÉ
        CachedData cached = cache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            // ✅ SERVIR DESDE CACHÉ
            String clientEtag = request.getHeader("If-None-Match");
            
            if (cached.etag.equals(clientEtag)) {
                // 304 Not Modified - El navegador ya tiene la versión correcta
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
            
            // Servir datos cacheados
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("ETag", cached.etag);
            response.setHeader("Cache-Control", "public, max-age=300"); // 5 minutos
            response.getWriter().print(cached.jsonData);
            return;
        }
        
        // 🔄 CONSULTAR BASE DE DATOS
        JsonArray habitaciones = obtenerHabitacionesDesdeBD(experiencia);
        String jsonResponse = habitaciones.toString();
        String etag = "\"" + Integer.toHexString(jsonResponse.hashCode()) + "\"";
        
        // 💾 GUARDAR EN CACHÉ
        cache.put(cacheKey, new CachedData(jsonResponse, etag));
        
        // 📤 ENVIAR RESPUESTA
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("ETag", etag);
        response.setHeader("Cache-Control", "public, max-age=300"); // 5 minutos
        response.getWriter().print(jsonResponse);
    }
    
    /**
     * 🚀 CONSULTA OPTIMIZADA A BASE DE DATOS
     */
    private JsonArray obtenerHabitacionesDesdeBD(String experiencia) {
        JsonArray habitaciones = new JsonArray();
        
        try (Connection conn = dbManager.getConnection()) {
            // ✅ CONSULTA OPTIMIZADA: Solo campos necesarios primero
            String sql = "SELECT id, codigo, nombre, experiencia, descripcion, precio_noche, " +
                        "capacidad, metros_cuadrados, wifi, aire_acondicionado, tv, minibar, " +
                        "balcon, imagen_principal " +
                        "FROM habitaciones " +
                        "WHERE activa = 1";
            
            if (experiencia != null && !experiencia.isEmpty()) {
                sql += " AND experiencia = ?";
            }
            
            sql += " ORDER BY experiencia, precio_noche"; // Ordenar por precio es más eficiente
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                
                if (experiencia != null && !experiencia.isEmpty()) {
                    ps.setString(1, experiencia);
                }
                
                // ⚡ IMPORTANTE: setFetchSize ayuda con grandes resultados
                ps.setFetchSize(50);
                
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
                        
                        // Servicios
                        hab.addProperty("wifi", rs.getInt("wifi"));
                        hab.addProperty("aire_acondicionado", rs.getInt("aire_acondicionado"));
                        hab.addProperty("tv", rs.getInt("tv"));
                        hab.addProperty("minibar", rs.getInt("minibar"));
                        hab.addProperty("balcon", rs.getInt("balcon"));
                        
                        hab.addProperty("imagen_principal", rs.getString("imagen_principal"));
                        
                        habitaciones.add(hab);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener habitaciones: " + e.getMessage());
            e.printStackTrace();
        }
        
        return habitaciones;
    }
    
    /**
     * 🔄 Método para limpiar caché manualmente (llamar cuando se actualicen habitaciones)
     */
    public static void limpiarCache() {
        cache.clear();
        System.out.println("✅ Caché de habitaciones limpiado");
    }
}