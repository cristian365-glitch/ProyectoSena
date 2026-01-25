package CRUDS;

import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/config")
public class ConfigServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // ⭐ SOLO exponer variables PÚBLICAS (Cloudinary)
        Map<String, String> config = new HashMap<>();
        config.put("cloudName", System.getenv("CLOUDINARY_CLOUD_NAME"));
        config.put("uploadPreset", System.getenv("CLOUDINARY_UPLOAD_PRESET"));
        
        Gson gson = new Gson();
        response.getWriter().write(gson.toJson(config));
    }
}