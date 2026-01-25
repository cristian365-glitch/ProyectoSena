package com.conexiones;

import com.pagos.MPConfiguracion;

public class GoogleOAuthConfig {
    
    private static final MPConfiguracion config = MPConfiguracion.getInstance();
    
    public static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    public static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    public static final String SCOPE = "openid email profile";
    
    public static String getClientId() {
        return config.getGoogleClientId();
    }
    
    public static String getClientSecret() {
        return config.getGoogleClientSecret();
    }
    
    public static String getRedirectUri() {
        return config.getGoogleRedirectUri();
    }
    
    public static void imprimirConfiguracion() {
        System.out.println("========================================");
        System.out.println("🔐 GOOGLE OAUTH CONFIGURATION");
        System.out.println("========================================");
        
        String clientId = getClientId();
        System.out.println(clientId != null ? "✅ Client ID: SET" : "❌ GOOGLE_CLIENT_ID no configurado");
        
        String clientSecret = getClientSecret();
        System.out.println(clientSecret != null ? "✅ Client Secret: SET" : "❌ GOOGLE_CLIENT_SECRET no configurado");
        
        System.out.println("✅ Redirect URI: " + getRedirectUri());
        System.out.println("✅ Scope: " + SCOPE);
        System.out.println("========================================");
    }
}