package CRUDS;

import com.google.gson.Gson;
import com.pagos.MPConfiguracion;

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
    private static final long serialVersionUID = 1L;
    private MPConfiguracion mpConfig;
    
    @Override
    public void init() throws ServletException {
        super.init();
        mpConfig = MPConfiguracion.getInstance();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // ⭐ SOLO exponer variables PÚBLICAS
        Map<String, String> config = new HashMap<>();
        config.put("cloudName", mpConfig.getCloudinaryCloudName());
        config.put("uploadPreset", mpConfig.getCloudinaryUploadPreset());
        
        Gson gson = new Gson();
        response.getWriter().write(gson.toJson(config));
    }
}
