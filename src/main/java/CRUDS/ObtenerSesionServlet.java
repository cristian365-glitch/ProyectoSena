package CRUDS;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/ObtenerSesionServlet")
public class ObtenerSesionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        Map<String, Object> resultado = new HashMap<>();
        
        if (session != null && session.getAttribute("logueado") != null) {
            // Usuario logueado
            resultado.put("logueado", true);
            resultado.put("nombre", session.getAttribute("nombre"));
            resultado.put("email", session.getAttribute("email"));
            resultado.put("telefono", session.getAttribute("telefono"));
            resultado.put("esAdmin", session.getAttribute("esAdmin"));
        } else {
            // No logueado
            resultado.put("logueado", false);
        }
        
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        out.print(gson.toJson(resultado));
        out.flush();
    }
}