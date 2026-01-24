package com.conexiones;

import com.pagos.MPConfiguracion;

/**
 * Configuración de Google OAuth 2.0
 * Ahora lee desde config.properties a través de MPConfiguracion
 */
public class GoogleOAuthConfig {
    
    private static final MPConfiguracion config = MPConfiguracion.getInstance();
    
    // URLs de Google OAuth (constantes)
    public static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    public static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    public static final String SCOPE = "openid email profile";
    
    public static String getClientId() {
        String clientId = config.getGoogleClientId();
        if (clientId == null || clientId.trim().isEmpty()) {
            System.err.println("⚠️ Google Client ID no configurado en config.properties");
            return null;
        }
        return clientId.trim();
    }
    
    public static String getClientSecret() {
        String secret = config.getGoogleClientSecret();
        if (secret == null || secret.trim().isEmpty()) {
            System.err.println("⚠️ Google Client Secret no configurado en config.properties");
            return null;
        }
        return secret.trim();
    }
    
    public static String getRedirectUri() {
        String uri = config.getGoogleRedirectUri();
        if (uri == null || uri.trim().isEmpty()) {
            System.err.println("⚠️ Google Redirect URI no configurado en config.properties");
            return null;
        }
        return uri.trim();
    }
    
    /**
     * Imprime la configuración actual (sin mostrar secretos completos)
     */
    public static void imprimirConfiguracion() {
        System.out.println("========================================");
        System.out.println("🔐 CONFIGURACIÓN DE GOOGLE OAUTH");
        System.out.println("========================================");
        
        String clientId = getClientId();
        if (clientId != null) {
            String preview = clientId.length() > 20 ? 
                clientId.substring(0, 20) + "..." : clientId;
            System.out.println("✅ Client ID: " + preview);
        } else {
            System.err.println("❌ Client ID NO configurado");
        }
        
        String clientSecret = getClientSecret();
        if (clientSecret != null) {
            System.out.println("✅ Client Secret: GOCSPX-***");
        } else {
            System.err.println("❌ Client Secret NO configurado");
        }
        
        System.out.println("Redirect URI: " + getRedirectUri());
        System.out.println("Scope: " + SCOPE);
        System.out.println("========================================");
    }
}