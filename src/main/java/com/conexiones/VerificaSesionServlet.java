package com.conexiones;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/VerificaSesionServlet")
public class VerificaSesionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Configurar CORS para permitir credenciales
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // No crear cache para esta respuesta
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        String action = request.getParameter("action");
        PrintWriter out = response.getWriter();
        
        if ("verificarSesion".equals(action)) {
            // NO crear sesión nueva si no existe, solo obtener la existente
            HttpSession session = request.getSession(false);
            
            // Verificar si hay sesión activa
            if (session != null && session.getAttribute("logueado") != null) {
                
                // Renovar el tiempo de expiración de la sesión
                session.setMaxInactiveInterval(30 * 60); // 30 minutos
                
                String usuario = (String) session.getAttribute("usuario");
                String email = (String) session.getAttribute("email");
                String fotoUrl = (String) session.getAttribute("foto_url");
                String loginMethod = (String) session.getAttribute("loginMethod");
                Integer userId = (Integer) session.getAttribute("userId");
                Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
                
                if (esAdmin == null) {
                    esAdmin = false;
                }
                
                // Log para debugging
                System.out.println("✅ Sesión válida: " + usuario + " (ID: " + userId + ")");
                
                // Construir JSON con toda la información de la sesión
                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"logueado\": true,");
                json.append("\"nombre\": \"").append(escapeJson(usuario)).append("\",");
                json.append("\"email\": \"").append(escapeJson(email)).append("\",");
                json.append("\"esAdmin\": ").append(esAdmin);
                
                // Incluir userId
                if (userId != null) {
                    json.append(",\"userId\": ").append(userId);
                }
                
                // Incluir URL de foto si existe
                if (fotoUrl != null && !fotoUrl.isEmpty()) {
                    json.append(",\"fotoUrl\": \"").append(escapeJson(fotoUrl)).append("\"");
                }
                
                // Incluir método de login si existe
                if (loginMethod != null && !loginMethod.isEmpty()) {
                    json.append(",\"loginMethod\": \"").append(escapeJson(loginMethod)).append("\"");
                }
                
                json.append("}");
                
                out.print(json.toString());
                
            } else {
                // No hay sesión activa
                System.out.println("❌ No hay sesión activa");
                out.print("{\"logueado\": false}");
            }
        } else if ("renovarSesion".equals(action)) {
            // Endpoint para renovar la sesión manualmente
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("logueado") != null) {
                session.setMaxInactiveInterval(30 * 60);
                out.print("{\"success\": true, \"message\": \"Sesión renovada\"}");
            } else {
                out.print("{\"success\": false, \"message\": \"No hay sesión activa\"}");
            }
        }
        
        out.flush();
        out.close();
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Manejar preflight requests de CORS
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    /**
     * Escapa caracteres especiales para JSON
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}