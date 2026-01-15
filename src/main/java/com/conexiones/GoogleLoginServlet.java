package com.conexiones;

import com.conexiones.GoogleOAuthConfig;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet que inicia el flujo de autenticación con Google OAuth 2.0
 * 
 * Flujo:
 * 1. Usuario hace clic en "Iniciar sesión con Google"
 * 2. Este servlet genera un state token (seguridad anti-CSRF)
 * 3. Redirige al usuario a Google para que autorice la aplicación
 * 4. Google redirige de vuelta a GoogleCallbackServlet con un código
 */
@WebServlet("/GoogleLoginServlet")
public class GoogleLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        System.out.println("🔵 Iniciando flujo de Google OAuth...");
        
        // Generar un state token para prevenir ataques CSRF
        // El state es un valor aleatorio que guardamos en sesión y verificamos después
        String state = generarStateToken();
        
        HttpSession session = request.getSession(true);
        session.setAttribute("google_oauth_state", state);
        
        // Construir la URL de autorización de Google
        StringBuilder authUrl = new StringBuilder();
        authUrl.append(GoogleOAuthConfig.AUTH_URL);
        authUrl.append("?client_id=").append(GoogleOAuthConfig.getClientId());
        authUrl.append("&redirect_uri=").append(GoogleOAuthConfig.getRedirectUri());
        authUrl.append("&response_type=code");
        authUrl.append("&scope=").append(GoogleOAuthConfig.SCOPE.replace(" ", "%20"));
        authUrl.append("&state=").append(state);
        
        // Parámetros adicionales opcionales:
        authUrl.append("&access_type=offline"); // Para obtener refresh token
        authUrl.append("&prompt=select_account"); // Siempre mostrar selector de cuenta
        
        System.out.println("🔵 Redirigiendo a Google OAuth...");
        System.out.println("   State token: " + state.substring(0, 10) + "...");
        
        // Redirigir al usuario a Google
        response.sendRedirect(authUrl.toString());
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Redirigir POST a GET
        doGet(request, response);
    }
    
    /**
     * Genera un token aleatorio y seguro para prevenir CSRF
     */
    private String generarStateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}