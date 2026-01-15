package com.conexiones;

/**
 * Configuración centralizada para Google OAuth 2.0
 * 
 * IMPORTANTE: En producción, estas credenciales deben estar en:
 * - Variables de entorno
 * - Archivos de configuración externos (.properties)
 * - Sistemas de gestión de secretos (AWS Secrets Manager, etc.)
 * 
 * NUNCA subir estas credenciales a Git
 */
public class GoogleOAuthConfig {
    
    // ⚠️ REEMPLAZAR CON TUS CREDENCIALES DE GOOGLE CLOUD CONSOLE
    public static final String CLIENT_ID = "401203717590-doj5m5st8l8lmm5f2997cddusbmvqfmc.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "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";
    
    // URL de redirección después de la autenticación
    // Debe coincidir EXACTAMENTE con la configurada en Google Cloud Console
    public static final String REDIRECT_URI = "http://localhost:8080/ProyectoSena/GoogleCallbackServlet";
    
    // En producción:
    // public static final String REDIRECT_URI = "https://tudominio.com/GoogleCallbackServlet";
    
    // Scopes (permisos) que solicitamos
    public static final String SCOPE = "openid email profile";
    
    // URLs de Google OAuth
    public static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    public static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    
    /**
     * Método alternativo: Cargar desde variables de entorno (RECOMENDADO)
     */
    public static String getClientId() {
        String envClientId = System.getenv("GOOGLE_CLIENT_ID");
        return (envClientId != null && !envClientId.isEmpty()) ? envClientId : CLIENT_ID;
    }
    
    public static String getClientSecret() {
        String envClientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
        return (envClientSecret != null && !envClientSecret.isEmpty()) ? envClientSecret : CLIENT_SECRET;
    }
    
    public static String getRedirectUri() {
        String envRedirectUri = System.getenv("GOOGLE_REDIRECT_URI");
        return (envRedirectUri != null && !envRedirectUri.isEmpty()) ? envRedirectUri : REDIRECT_URI;
    }
}