package com.pagos;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MPConfiguracion {
    private static MPConfiguracion instance;
    private Properties properties;
    private boolean archivoEncontrado = false;
    
    private MPConfiguracion() {
        properties = new Properties();
        cargarConfiguracion();
    }
    
    private void cargarConfiguracion() {
        try {
            InputStream input = getClass().getClassLoader()
                    .getResourceAsStream("mercadopago.properties");
            
            if (input != null) {
                properties.load(input);
                archivoEncontrado = true;
                
                System.out.println("✅ ========================================");
                System.out.println("✅ Archivo mercadopago.properties ENCONTRADO");
                System.out.println("✅ ========================================");
                
                String token = properties.getProperty("mercadopago.access_token.test");
                String pubKey = properties.getProperty("mercadopago.public_key.test");
                String modo = properties.getProperty("mercadopago.modo");
                
                System.out.println("📋 Propiedades cargadas:");
                System.out.println("   - Modo: " + modo);
                System.out.println("   - Access Token length: " + (token != null ? token.length() : "NULL"));
                System.out.println("   - Public Key length: " + (pubKey != null ? pubKey.length() : "NULL"));
                
                if (token == null || token.trim().isEmpty()) {
                    System.err.println("⚠️ WARNING: mercadopago.access_token.test está VACÍO");
                    System.err.println("⚠️ Edita el archivo: resources/mercadopago.properties");
                }
                
                if (pubKey == null || pubKey.trim().isEmpty()) {
                    System.err.println("⚠️ WARNING: mercadopago.public_key.test está VACÍO");
                }
                
                input.close();
                
            } else {
                System.err.println("❌ ========================================");
                System.err.println("❌ Archivo mercadopago.properties NO encontrado");
                System.err.println("❌ ========================================");
                archivoEncontrado = false;
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error al cargar mercadopago.properties");
            e.printStackTrace();
            archivoEncontrado = false;
        }
    }
    
    public static MPConfiguracion getInstance() {
        if (instance == null) {
            instance = new MPConfiguracion();
        }
        return instance;
    }
    
    public String getAccessToken() {
        String modo = properties.getProperty("mercadopago.modo", "test");
        String token = properties.getProperty("mercadopago.access_token." + modo);
        
        if (token == null || token.trim().isEmpty()) {
            System.err.println("⚠️ WARNING: Mercado Pago Access Token no configurado");
            System.err.println("⚠️ Modo actual: " + modo);
            System.err.println("⚠️ Propiedad buscada: mercadopago.access_token." + modo);
            System.err.println("⚠️ Archivo encontrado: " + archivoEncontrado);
            
            if (archivoEncontrado) {
                System.err.println("⚠️ El archivo existe pero la propiedad está VACÍA");
                System.err.println("⚠️ Abre: resources/mercadopago.properties");
                System.err.println("⚠️ Y agrega tu Access Token después del =");
            }
            
            return null;
        }
        
        String preview = token.length() > 20 ? token.substring(0, 20) + "..." : token;
        System.out.println("✅ Access Token cargado: " + preview + " (length: " + token.length() + ")");
        
        return token.trim();
    }
    
    public String getPublicKey() {
        String modo = properties.getProperty("mercadopago.modo", "test");
        String key = properties.getProperty("mercadopago.public_key." + modo);
        
        if (key != null && !key.trim().isEmpty()) {
            String preview = key.length() > 20 ? key.substring(0, 20) + "..." : key;
            System.out.println("✅ Public Key cargado: " + preview);
            return key.trim();
        }
        
        System.err.println("⚠️ Public Key no configurado");
        return null;
    }
    
    /**
     * ⭐ Obtiene la URL base configurada
     */
    public String getBaseUrl() {
        return properties.getProperty("mercadopago.urls.base", 
                "http://localhost:8080/ProyectoSena");
    }
    
    /**
     * ⭐ Construye la URL de éxito con token y reserva
     */
    public String getSuccessUrl(String token, String reservaId) {
        String base = getBaseUrl();
        String path = properties.getProperty("mercadopago.urls.success", "/pago-exitoso.html");
        return String.format("%s%s?token=%s&reserva=%s", base, path, token, reservaId);
    }
    
    /**
     * ⭐ Construye la URL de fallo con token y reserva
     */
    public String getFailureUrl(String token, String reservaId) {
        String base = getBaseUrl();
        String path = properties.getProperty("mercadopago.urls.failure", "/pago-fallido.html");
        return String.format("%s%s?token=%s&reserva=%s", base, path, token, reservaId);
    }
    
    /**
     * ⭐ Construye la URL de pendiente con token y reserva
     */
    public String getPendingUrl(String token, String reservaId) {
        String base = getBaseUrl();
        String path = properties.getProperty("mercadopago.urls.pending", "/pago-pendiente.html");
        return String.format("%s%s?token=%s&reserva=%s", base, path, token, reservaId);
    }
    
    /**
     * URLs sin token (para compatibilidad hacia atrás)
     */
    public String getSuccessUrl() {
        return getBaseUrl() + properties.getProperty("mercadopago.urls.success", "/pago-exitoso.html");
    }
    
    public String getFailureUrl() {
        return getBaseUrl() + properties.getProperty("mercadopago.urls.failure", "/pago-fallido.html");
    }
    
    public String getPendingUrl() {
        return getBaseUrl() + properties.getProperty("mercadopago.urls.pending", "/pago-pendiente.html");
    }
    
    public String getWebhookUrl() {
        return properties.getProperty("mercadopago.webhook.url", 
                "http://localhost:8080/ProyectoSena/webhook/mercadopago");
    }
    
    public String getWebhookSecret() {
        return properties.getProperty("mercadopago.webhook.secret");
    }
    
    public boolean isModoTest() {
        return "test".equals(properties.getProperty("mercadopago.modo", "test"));
    }
    
    public void imprimirConfiguracion() {
        System.out.println("=== CONFIGURACIÓN MERCADO PAGO ===");
        System.out.println("Archivo encontrado: " + archivoEncontrado);
        System.out.println("Modo: " + properties.getProperty("mercadopago.modo"));
        
        String token = getAccessToken();
        System.out.println("Access Token: " + (token != null && !token.isEmpty() ? "✅ Configurado" : "❌ NO configurado"));
        
        String pubKey = getPublicKey();
        System.out.println("Public Key: " + (pubKey != null && !pubKey.isEmpty() ? "✅ Configurado" : "❌ NO configurado"));
        
        System.out.println("Base URL: " + getBaseUrl());
        System.out.println("Success Path: " + properties.getProperty("mercadopago.urls.success"));
        System.out.println("Failure Path: " + properties.getProperty("mercadopago.urls.failure"));
        System.out.println("Pending Path: " + properties.getProperty("mercadopago.urls.pending"));
        System.out.println("Webhook URL: " + getWebhookUrl());
        System.out.println("==================================");
    }
}