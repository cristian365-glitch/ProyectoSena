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
            // ✅ AHORA LEE DE config.properties (unificado)
            InputStream input = getClass().getClassLoader()
                    .getResourceAsStream("config.properties");
            
            if (input != null) {
                properties.load(input);
                archivoEncontrado = true;
                
                System.out.println("✅ ========================================");
                System.out.println("✅ Archivo config.properties ENCONTRADO");
                System.out.println("✅ ========================================");
                
                String modo = properties.getProperty("mercadopago.modo", "test");
                String token = properties.getProperty("mercadopago.access_token." + modo);
                String pubKey = properties.getProperty("mercadopago.public_key." + modo);
                
                System.out.println("📋 Configuración de Mercado Pago:");
                System.out.println("   - Modo: " + modo);
                System.out.println("   - Access Token: " + (token != null && !token.isEmpty() ? "✅ Configurado" : "❌ VACÍO"));
                System.out.println("   - Public Key: " + (pubKey != null && !pubKey.isEmpty() ? "✅ Configurado" : "❌ VACÍO"));
                
                if (token == null || token.trim().isEmpty()) {
                    System.err.println("⚠️ WARNING: mercadopago.access_token." + modo + " está VACÍO");
                    System.err.println("⚠️ Edita: resources/config.properties");
                }
                
                input.close();
                
            } else {
                System.err.println("❌ ========================================");
                System.err.println("❌ Archivo config.properties NO encontrado");
                System.err.println("❌ Crea: src/main/resources/config.properties");
                System.err.println("❌ ========================================");
                archivoEncontrado = false;
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error al cargar config.properties");
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
            System.err.println("⚠️ Propiedad: mercadopago.access_token." + modo);
            return null;
        }
        
        return token.trim();
    }
    
    public String getPublicKey() {
        String modo = properties.getProperty("mercadopago.modo", "test");
        String key = properties.getProperty("mercadopago.public_key." + modo);
        
        if (key != null && !key.trim().isEmpty()) {
            return key.trim();
        }
        
        System.err.println("⚠️ Public Key no configurado");
        return null;
    }
    
    public String getBaseUrl() {
        return properties.getProperty("mercadopago.urls.base", 
                "http://localhost:8080/ProyectoSena");
    }
    
    public String getSuccessUrl(String token, String reservaId) {
        String base = getBaseUrl();
        String path = properties.getProperty("mercadopago.urls.success", "/pago-exitoso.html");
        return String.format("%s%s?token=%s&reserva=%s", base, path, token, reservaId);
    }
    
    public String getFailureUrl(String token, String reservaId) {
        String base = getBaseUrl();
        String path = properties.getProperty("mercadopago.urls.failure", "/pago-fallido.html");
        return String.format("%s%s?token=%s&reserva=%s", base, path, token, reservaId);
    }
    
    public String getPendingUrl(String token, String reservaId) {
        String base = getBaseUrl();
        String path = properties.getProperty("mercadopago.urls.pending", "/pago-pendiente.html");
        return String.format("%s%s?token=%s&reserva=%s", base, path, token, reservaId);
    }
    
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
    
    // ================================================
    // ✅ NUEVOS MÉTODOS PARA GOOGLE OAUTH
    // ================================================
    
    public String getGoogleClientId() {
        return properties.getProperty("google.client_id");
    }
    
    public String getGoogleClientSecret() {
        return properties.getProperty("google.client_secret");
    }
    
    public String getGoogleRedirectUri() {
        return properties.getProperty("google.redirect_uri");
    }
    
    public String getGoogleScope() {
        return properties.getProperty("google.scope", "openid email profile");
    }
    
    public String getGoogleAuthUrl() {
        return properties.getProperty("google.auth_url", 
                "https://accounts.google.com/o/oauth2/v2/auth");
    }
    
    public String getGoogleTokenUrl() {
        return properties.getProperty("google.token_url", 
                "https://oauth2.googleapis.com/token");
    }
    
    public String getGoogleUserInfoUrl() {
        return properties.getProperty("google.userinfo_url", 
                "https://www.googleapis.com/oauth2/v3/userinfo");
    }
    
    public void imprimirConfiguracion() {
        System.out.println("========================================");
        System.out.println("📋 CONFIGURACIÓN COMPLETA");
        System.out.println("========================================");
        System.out.println("Archivo encontrado: " + (archivoEncontrado ? "✅ SÍ" : "❌ NO"));
        
        if (archivoEncontrado) {
            // Mercado Pago
            System.out.println("\n💳 MERCADO PAGO:");
            System.out.println("   Modo: " + properties.getProperty("mercadopago.modo"));
            
            String token = getAccessToken();
            System.out.println("   Access Token: " + (token != null && !token.isEmpty() ? "✅ OK" : "❌ NO"));
            
            String pubKey = getPublicKey();
            System.out.println("   Public Key: " + (pubKey != null && !pubKey.isEmpty() ? "✅ OK" : "❌ NO"));
            System.out.println("   Base URL: " + getBaseUrl());
            
            // Google OAuth
            System.out.println("\n🔐 GOOGLE OAUTH:");
            String clientId = getGoogleClientId();
            System.out.println("   Client ID: " + (clientId != null && !clientId.isEmpty() ? "✅ OK" : "❌ NO"));
            
            String clientSecret = getGoogleClientSecret();
            System.out.println("   Client Secret: " + (clientSecret != null && !clientSecret.isEmpty() ? "✅ OK" : "❌ NO"));
            System.out.println("   Redirect URI: " + getGoogleRedirectUri());
        }
        
        System.out.println("========================================");
    }
}