package com.pagos;

import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@WebServlet("/admin/crear-usuario-prueba-mp")
public class CrearUsuarioPruebaMPServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        com.pagos.MPConfiguracion mpConfig = com.pagos.MPConfiguracion.getInstance();
        
        try {
            URL url = new URL("https://api.mercadopago.com/users/test");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + mpConfig.getAccessToken());
            conn.setDoOutput(true);
            
            JsonObject body = new JsonObject();
            body.addProperty("site_id", "MCO"); // Colombia
            body.addProperty("description", "Usuario de prueba Hotel Armonía");
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 201) {
                response.getWriter().print("{\"success\": true, \"message\": \"Usuario de prueba creado\"}");
            } else {
                response.getWriter().print("{\"success\": false, \"error\": \"Error al crear usuario\"}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }
}