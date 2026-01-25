package com.conexiones;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
 
    // ⭐ LEER DESDE VARIABLES DE ENTORNO DE RENDER
    private static final String URL = System.getenv("URL");
    private static final String USER = System.getenv("USER");
    private static final String PASSWORD = System.getenv("PASSWORD");
    
    private static DatabaseManager instance;
    
    private DatabaseManager() {
        // Validar que las variables existan
        if (URL == null || USER == null || PASSWORD == null) {
            System.err.println("========================================");
            System.err.println("❌ ERROR: Variables de entorno no configuradas");
            System.err.println("========================================");
            System.err.println("Debes configurar en Render:");
            System.err.println("  - DB_URL");
            System.err.println("  - DB_USER");
            System.err.println("  - DB_PASSWORD");
            System.err.println("========================================");
        } else {
            System.out.println("========================================");
            System.out.println("✅ DATABASE CONFIGURATION");
            System.out.println("========================================");
            System.out.println("URL: " + URL);
            System.out.println("User: " + USER);
            System.out.println("Password: ✅ SET");
            System.out.println("========================================");
        }
    }
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado", e);
        }
    }
    
    public static void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("Error al cerrar recurso: " + e.getMessage());
                }
            }
        }
    }
    
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error al probar conexión: " + e.getMessage());
            return false;
        }
    }
}
