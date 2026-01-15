package CRUDS;

import com.conexiones.DatabaseManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/EditarHabitacionServlet")
public class EditarHabitacionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
        
        if (!dbManager.testConnection()) {
            throw new ServletException("No se puede conectar a la base de datos");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Verificar que sea admin
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("logueado") == null) {
            response.sendRedirect(request.getContextPath() + "/Login.html?error=no_logueado");
            return;
        }
        
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        
        if (esAdmin == null || !esAdmin) {
            response.sendRedirect(request.getContextPath() + "../index.html?error=no_autorizado");
            return;
        }
        
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        // Obtener ID de la habitación a editar
        String idStr = request.getParameter("id");
        String nombre = request.getParameter("nombre");
        String experiencia = request.getParameter("experiencia");
        String descripcion = request.getParameter("descripcion");
        String precioStr = request.getParameter("precio");
        String capacidadStr = request.getParameter("capacidad");
        String imagenesJson = request.getParameter("imagenes");
        
        // Nuevos parámetros
        String metrosStr = request.getParameter("metros");
        String vista = request.getParameter("vista");
        String camasInfo = request.getParameter("camas");
        boolean wifi = request.getParameter("wifi") != null;
        boolean aire = request.getParameter("aire") != null;
        boolean tv = request.getParameter("tv") != null;
        boolean minibar = request.getParameter("minibar") != null;
        boolean balcon = request.getParameter("balcon") != null;
        boolean desayuno = request.getParameter("desayuno") != null;
        
        // Validaciones
        if (idStr == null || nombre == null || nombre.trim().isEmpty() ||
            experiencia == null || experiencia.trim().isEmpty() ||
            precioStr == null || capacidadStr == null) {
            response.sendRedirect(request.getContextPath() + "/editar-habitacion.html?id=" + idStr + "&error=campos_vacios");
            return;
        }
        
        try {
            int id = Integer.parseInt(idStr);
            double precio = Double.parseDouble(precioStr);
            int capacidad = Integer.parseInt(capacidadStr);
            Integer metros = (metrosStr != null && !metrosStr.isEmpty()) ? Integer.parseInt(metrosStr) : null;
            
            Connection conn = null;
            PreparedStatement psUpdate = null;
            PreparedStatement psDeleteImages = null;
            PreparedStatement psInsertImages = null;
            
            try {
                conn = dbManager.getConnection();
                conn.setAutoCommit(false);
                
                // Actualizar habitación
                String updateSql = "UPDATE habitaciones SET " +
                                  "nombre = ?, experiencia = ?, descripcion = ?, precio_noche = ?, capacidad = ?, " +
                                  "metros_cuadrados = ?, wifi = ?, aire_acondicionado = ?, tv = ?, minibar = ?, " +
                                  "balcon = ?, vista = ?, camas_info = ?, desayuno_incluido = ?, imagen_principal = ? " +
                                  "WHERE id = ?";
                
                psUpdate = conn.prepareStatement(updateSql);
                
                psUpdate.setString(1, nombre.trim());
                psUpdate.setString(2, experiencia.trim());
                psUpdate.setString(3, descripcion != null ? descripcion.trim() : null);
                psUpdate.setDouble(4, precio);
                psUpdate.setInt(5, capacidad);
                
                if (metros != null) {
                    psUpdate.setInt(6, metros);
                } else {
                    psUpdate.setNull(6, java.sql.Types.INTEGER);
                }
                psUpdate.setBoolean(7, wifi);
                psUpdate.setBoolean(8, aire);
                psUpdate.setBoolean(9, tv);
                psUpdate.setBoolean(10, minibar);
                psUpdate.setBoolean(11, balcon);
                psUpdate.setString(12, vista != null && !vista.isEmpty() ? vista : null);
                psUpdate.setString(13, camasInfo != null && !camasInfo.isEmpty() ? camasInfo.trim() : null);
                psUpdate.setBoolean(14, desayuno);
                
                // Si se proporcionaron nuevas imágenes, actualizar imagen principal
                if (imagenesJson != null && !imagenesJson.trim().isEmpty()) {
                    List<String> imagenes = parsearImagenes(imagenesJson);
                    if (!imagenes.isEmpty()) {
                        psUpdate.setString(15, imagenes.get(0));
                        
                        // Eliminar imágenes antiguas
                        String deleteSql = "DELETE FROM imagenes_habitacion WHERE habitacion_id = ?";
                        psDeleteImages = conn.prepareStatement(deleteSql);
                        psDeleteImages.setInt(1, id);
                        psDeleteImages.executeUpdate();
                        
                        // Insertar nuevas imágenes
                        String insertImagenSql = "INSERT INTO imagenes_habitacion (habitacion_id, url_cloudinary, orden, es_principal) VALUES (?, ?, ?, ?)";
                        psInsertImages = conn.prepareStatement(insertImagenSql);
                        
                        for (int i = 0; i < imagenes.size(); i++) {
                            psInsertImages.setInt(1, id);
                            psInsertImages.setString(2, imagenes.get(i));
                            psInsertImages.setInt(3, i);
                            psInsertImages.setBoolean(4, i == 0);
                            psInsertImages.addBatch();
                        }
                        
                        psInsertImages.executeBatch();
                    }
                } else {
                    // No cambiar imagen principal si no hay nuevas imágenes
                    psUpdate.setString(15, obtenerImagenPrincipal(conn, id));
                }
                
                psUpdate.setInt(16, id);
                
                int filas = psUpdate.executeUpdate();
                
                if (filas > 0) {
                    conn.commit();
                    response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/gestionar-habitaciones.html?exito=actualizado");
                } else {
                    conn.rollback();
                    response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/editar-habitacion.html?id=" + id + "&error=no_actualizado");
                }
                
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/editar-habitacion.html?id=" + idStr + "&error=error_sistema");
            } finally {
                DatabaseManager.closeResources(null, psUpdate, psDeleteImages, psInsertImages, conn);
            }
            
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/editar-habitacion.html?id=" + idStr + "&error=datos_invalidos");
        }
    }
    
    private String obtenerImagenPrincipal(Connection conn, int habitacionId) throws SQLException {
        String sql = "SELECT imagen_principal FROM habitaciones WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, habitacionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("imagen_principal");
                }
            }
        }
        return null;
    }
    
    private List<String> parsearImagenes(String json) {
        List<String> imagenes = new ArrayList<>();
        
        json = json.trim().replaceAll("^\\[|\\]$", "");
        
        String[] urls = json.split("\",\"");
        
        for (String url : urls) {
            url = url.trim()
                    .replaceAll("^\"|\"$", "")
                    .replaceAll("^\\{|\\}$", "")
                    .trim();
            
            if (!url.isEmpty() && url.startsWith("http")) {
                imagenes.add(url);
            }
        }
        
        return imagenes;
    }
}