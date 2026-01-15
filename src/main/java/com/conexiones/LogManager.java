package com.conexiones;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.servlet.http.HttpServletRequest;

/**
 * Clase simple para registrar logs en la tabla login_log
 */
public class LogManager {
    
    private static DatabaseManager dbManager = DatabaseManager.getInstance();
    
    /**
     * Registra cualquier acción en la base de datos
     * @param userId - ID del usuario
     * @param ipAddress - Dirección IP
     * @param loginTime - Fecha y hora del evento
     * @param esAdmin - Si es administrador
     */
    public static void registrarLog(int userId, String ipAddress, boolean esAdmin) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = dbManager.getConnection();
            
            String sql = "INSERT INTO login_log (user_id, ip_address, login_time, es_admin) VALUES (?, ?, ?, ?)";
            
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, ipAddress);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setBoolean(4, esAdmin);
            
            ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error al registrar log: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseManager.closeResources(null, ps, conn);
        }
    }
    
    /**
     * Obtiene la dirección IP del cliente
     */
    public static String obtenerIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        return ip != null ? ip : "0.0.0.0";
    }
}