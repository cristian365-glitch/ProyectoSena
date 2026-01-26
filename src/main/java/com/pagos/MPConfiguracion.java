package com.pagos;

public class MPConfiguracion {
    private static MPConfiguracion instance;
    
    private MPConfiguracion() {
        imprimirConfiguracion();
    }
    
    public static MPConfiguracion getInstance() {
        if (instance == null) {
            instance = new MPConfiguracion();
        }
        return instance;
    }
    
    // ============================================
    // MERCADOPAGO
    // ============================================
    
    public String getAccessToken() {
        return System.getenv("MERCADOPAGO_ACCESS_TOKEN");
    }
    
    public String getPublicKey() {
        return System.getenv("MERCADOPAGO_PUBLIC_KEY");
    }
    
    /**
     * ⭐ ACTUALIZADO: Reconoce tanto "test" como "prod"
     */
    public boolean isModoTest() {
        String modo = System.getenv("MERCADOPAGO_MODO");
        
        // Si no está definido, por defecto es TEST
        if (modo == null || modo.trim().isEmpty()) {
            return true;
        }
        
        // Si es "prod" o "production" → PRODUCCIÓN (false)
        // Si es "test" → TEST (true)
        return !"prod".equalsIgnoreCase(modo) && !"production".equalsIgnoreCase(modo);
    }
    
    // ============================================
    // URLs BASE
    // ============================================
    
    public String getBaseUrl() {
        String url = System.getenv("BASE_URL");
        return url != null ? url : "http://localhost:8080";
    }
    
    // ============================================
    // URLs DE RETORNO (con token de sesión)
    // ============================================
    
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
    
    // ============================================
    // WEBHOOK
    // ============================================
    
    public String getWebhookUrl() {
        return System.getenv("WEBHOOK_URL");
    }
    
    public String getWebhookSecret() {
        return System.getenv("MERCADOPAGO_WEBHOOK_SECRET");
    }
    
    // ============================================
    // GOOGLE OAUTH
    // ============================================
    
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
    
    // ============================================
    // CLOUDINARY
    // ============================================
    
    public String getCloudinaryCloudName() {
        return System.getenv("CLOUDINARY_CLOUD_NAME");
    }
    
    public String getCloudinaryUploadPreset() {
        return System.getenv("UPLOAD_PRESET");
    }
    
    // ============================================
    // DIAGNÓSTICO
    // ============================================
    
    public void imprimirConfiguracion() {
        System.out.println("========================================");
        System.out.println("📋 CONFIGURACIÓN COMPLETA");
        System.out.println("========================================");
        
        // Mercado Pago
        System.out.println("\n💳 MERCADO PAGO:");
        String token = getAccessToken();
        System.out.println("   Access Token: " + (token != null && !token.isEmpty() ? "✅ OK (" + token.substring(0, Math.min(15, token.length())) + "...)" : "❌ NO CONFIGURADO"));
        
        String pubKey = getPublicKey();
        System.out.println("   Public Key: " + (pubKey != null && !pubKey.isEmpty() ? "✅ OK (" + pubKey.substring(0, Math.min(15, pubKey.length())) + "...)" : "❌ NO CONFIGURADO"));
        
        String modoEnv = System.getenv("MERCADOPAGO_MODO");
        System.out.println("   Variable MERCADOPAGO_MODO: " + (modoEnv != null ? modoEnv : "NO DEFINIDA (usará TEST)"));
        System.out.println("   Modo detectado: " + (isModoTest() ? "🧪 TEST (Sandbox)" : "🚀 PRODUCCIÓN"));
        System.out.println("   Base URL: " + getBaseUrl());
        
        String webhookUrl = getWebhookUrl();
        System.out.println("   Webhook URL: " + (webhookUrl != null ? "✅ " + webhookUrl : "❌ NO CONFIGURADO"));
        
        String webhookSecret = getWebhookSecret();
        System.out.println("   Webhook Secret: " + (webhookSecret != null && !webhookSecret.isEmpty() ? "✅ OK" : "⚠️ NO CONFIGURADO (validación deshabilitada)"));
        
        // Google OAuth
        System.out.println("\n🔐 GOOGLE OAUTH:");
        String clientId = getGoogleClientId();
        System.out.println("   Client ID: " + (clientId != null && !clientId.isEmpty() ? "✅ OK" : "❌ NO CONFIGURADO"));
        
        String clientSecret = getGoogleClientSecret();
        System.out.println("   Client Secret: " + (clientSecret != null && !clientSecret.isEmpty() ? "✅ OK" : "❌ NO CONFIGURADO"));
        System.out.println("   Redirect URI: " + getGoogleRedirectUri());
        
        // Cloudinary
        System.out.println("\n☁️ CLOUDINARY:");
        String cloudName = getCloudinaryCloudName();
        System.out.println("   Cloud Name: " + (cloudName != null && !cloudName.isEmpty() ? "✅ OK" : "❌ NO CONFIGURADO"));
        
        String uploadPreset = getCloudinaryUploadPreset();
        System.out.println("   Upload Preset: " + (uploadPreset != null && !uploadPreset.isEmpty() ? "✅ OK" : "❌ NO CONFIGURADO"));
        
        System.out.println("========================================");
    }
}