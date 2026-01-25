package com.pagos;

public class MPConfiguracion {
    private static MPConfiguracion instance;
    
    private MPConfiguracion() {
        validarConfiguracion();
    }
    
    private void validarConfiguracion() {
        System.out.println("========================================");
        System.out.println("💳 MERCADOPAGO CONFIGURATION");
        System.out.println("========================================");
        
        String token = getAccessToken();
        String pubKey = getPublicKey();
        String baseUrl = getBaseUrl();
        
        System.out.println(token != null ? "✅ Access Token: SET" : "❌ MERCADOPAGO_ACCESS_TOKEN no configurado");
        System.out.println(pubKey != null ? "✅ Public Key: SET" : "❌ MERCADOPAGO_PUBLIC_KEY no configurado");
        System.out.println("✅ Base URL: " + baseUrl);
        System.out.println("✅ Modo: " + (isModoTest() ? "TEST" : "PRODUCCIÓN"));
        System.out.println("========================================");
    }
    
    public static MPConfiguracion getInstance() {
        if (instance == null) {
            instance = new MPConfiguracion();
        }
        return instance;
    }
    
    public String getAccessToken() {
        return System.getenv("MERCADOPAGO_ACCESS_TOKEN");
    }
    
    public String getPublicKey() {
        return System.getenv("MERCADOPAGO_PUBLIC_KEY");
    }
    
    public String getBaseUrl() {
        String url = System.getenv("BASE_URL");
        return url != null ? url : "http://localhost:8080/ProyectoSena";
    }
    
    public String getSuccessUrl(String token, String reservaId) {
        return String.format("%s/pago-exitoso.html?token=%s&reserva=%s", 
            getBaseUrl(), token, reservaId);
    }
    
    public String getFailureUrl(String token, String reservaId) {
        return String.format("%s/pago-fallido.html?token=%s&reserva=%s", 
            getBaseUrl(), token, reservaId);
    }
    
    public String getPendingUrl(String token, String reservaId) {
        return String.format("%s/pago-pendiente.html?token=%s&reserva=%s", 
            getBaseUrl(), token, reservaId);
    }
    
    public String getSuccessUrl() {
        return getBaseUrl() + "/pago-exitoso.html";
    }
    
    public String getFailureUrl() {
        return getBaseUrl() + "/pago-fallido.html";
    }
    
    public String getPendingUrl() {
        return getBaseUrl() + "/pago-pendiente.html";
    }
    
    public String getWebhookUrl() {
        String webhook = System.getenv("WEBHOOK_URL");
        return webhook != null ? webhook : getBaseUrl() + "/webhook/mercadopago";
    }
    
    public String getWebhookSecret() {
        return System.getenv("MERCADOPAGO_WEBHOOK_SECRET");
    }
    
    public boolean isModoTest() {
        String modo = System.getenv("MERCADOPAGO_MODO");
        return modo == null || "test".equals(modo);
    }
    
    // ✅ MÉTODOS PARA GOOGLE OAUTH
    public String getGoogleClientId() {
        return System.getenv("GOOGLE_CLIENT_ID");
    }
    
    public String getGoogleClientSecret() {
        return System.getenv("GOOGLE_CLIENT_SECRET");
    }
    
    public String getGoogleRedirectUri() {
        return getBaseUrl() + "/GoogleCallbackServlet";
    }
    
    public String getGoogleScope() {
        return "openid email profile";
    }
    
    public String getGoogleAuthUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth";
    }
    
    public String getGoogleTokenUrl() {
        return "https://oauth2.googleapis.com/token";
    }
    
    public String getGoogleUserInfoUrl() {
        return "https://www.googleapis.com/oauth2/v3/userinfo";
    }
    
    public void imprimirConfiguracion() {
        System.out.println("========================================");
        System.out.println("📋 CONFIGURACIÓN COMPLETA");
        System.out.println("========================================");
        
        // Mercado Pago
        System.out.println("\n💳 MERCADO PAGO:");
        String token = getAccessToken();
        System.out.println("   Access Token: " + (token != null ? "✅ OK" : "❌ NO"));
        
        String pubKey = getPublicKey();
        System.out.println("   Public Key: " + (pubKey != null ? "✅ OK" : "❌ NO"));
        System.out.println("   Base URL: " + getBaseUrl());
        System.out.println("   Modo: " + (isModoTest() ? "TEST" : "PRODUCCIÓN"));
        
        // Google OAuth
        System.out.println("\n🔐 GOOGLE OAUTH:");
        String clientId = getGoogleClientId();
        System.out.println("   Client ID: " + (clientId != null ? "✅ OK" : "❌ NO"));
        
        String clientSecret = getGoogleClientSecret();
        System.out.println("   Client Secret: " + (clientSecret != null ? "✅ OK" : "❌ NO"));
        System.out.println("   Redirect URI: " + getGoogleRedirectUri());
        
        System.out.println("========================================");
    }
}