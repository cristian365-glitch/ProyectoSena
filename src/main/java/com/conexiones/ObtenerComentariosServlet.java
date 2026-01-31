// ObtenerComentariosServlet.java
// Servlet para obtener comentarios y puntuaciones de habitaciones

package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import conexion.ConexionBD;

@WebServlet("/ObtenerComentariosServlet")
public class ObtenerComentariosServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        List<Comentario> comentarios = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConexionBD.getConnection();
            
            // Consulta para obtener comentarios aprobados
            String sql = "SELECT c.id, c.habitacion_id, c.usuario_id, c.comentario, " +
                        "c.puntuacion, c.fecha_comentario, u.nombre as nombre_usuario " +
                        "FROM comentarios c " +
                        "INNER JOIN usuarios u ON c.usuario_id = u.id " +
                        "WHERE c.aprobado = true " +
                        "ORDER BY c.fecha_comentario DESC";
            
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Comentario comentario = new Comentario();
                comentario.setId(rs.getInt("id"));
                comentario.setHabitacionId(rs.getInt("habitacion_id"));
                comentario.setUsuarioId(rs.getInt("usuario_id"));
                comentario.setComentario(rs.getString("comentario"));
                comentario.setPuntuacion(rs.getInt("puntuacion"));
                comentario.setFechaComentario(rs.getTimestamp("fecha_comentario"));
                comentario.setNombreUsuario(rs.getString("nombre_usuario"));
                
                comentarios.add(comentario);
            }
            
            // Convertir a JSON
            Gson gson = new Gson();
            String json = gson.toJson(comentarios);
            out.print(json);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error al obtener comentarios\"}");
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

// Clase modelo para Comentario
class Comentario {
    private int id;
    private int habitacionId;
    private int usuarioId;
    private String comentario;
    private int puntuacion;
    private java.sql.Timestamp fechaComentario;
    private String nombreUsuario;
    
    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getHabitacionId() { return habitacionId; }
    public void setHabitacionId(int habitacionId) { this.habitacionId = habitacionId; }
    
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    
    public int getPuntuacion() { return puntuacion; }
    public void setPuntuacion(int puntuacion) { this.puntuacion = puntuacion; }
    
    public java.sql.Timestamp getFechaComentario() { return fechaComentario; }
    public void setFechaComentario(java.sql.Timestamp fechaComentario) { 
        this.fechaComentario = fechaComentario; 
    }
    
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { 
        this.nombreUsuario = nombreUsuario; 
    }
}