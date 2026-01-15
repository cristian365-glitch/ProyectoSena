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

@WebServlet("/GuardarHabitacionServlet")
public class GuardarHabitacionServlet extends HttpServlet {
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
            response.sendRedirect(request.getContextPath() + "../Login.html?error=no_logueado");
            return;
        }
        
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        
        if (esAdmin == null || !esAdmin) {
            response.sendRedirect(request.getContextPath() + "../index.html?error=no_autorizado");
            return;
        }
        
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        // Obtener parámetros básicos
        String codigo = request.getParameter("codigo");
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
        if (codigo == null || codigo.trim().isEmpty() ||
            nombre == null || nombre.trim().isEmpty() ||
            experiencia == null || experiencia.trim().isEmpty() ||
            precioStr == null || capacidadStr == null ||
            imagenesJson == null || imagenesJson.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/admin-habitaciones.html?error=campos_vacios");
            return;
        }
        
        try {
            double precio = Double.parseDouble(precioStr);
            int capacidad = Integer.parseInt(capacidadStr);
            Integer metros = (metrosStr != null && !metrosStr.isEmpty()) ? Integer.parseInt(metrosStr) : null;
            
            List<String> imagenes = parsearImagenes(imagenesJson);
            
            if (imagenes.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/admin-habitaciones.html?error=sin_imagenes");
                return;
            }
            
            Connection conn = null;
            PreparedStatement psCheck = null;
            PreparedStatement psInsert = null;
            PreparedStatement psImagenes = null;
            ResultSet rs = null;
            
            try {
                conn = dbManager.getConnection();
                conn.setAutoCommit(false);
                
                // Verificar que el código no exista
                String checkSql = "SELECT COUNT(*) FROM habitaciones WHERE codigo = ?";
                psCheck = conn.prepareStatement(checkSql);
                psCheck.setString(1, codigo.trim().toUpperCase());
                rs = psCheck.executeQuery();
                
                if (rs.next() && rs.getInt(1) > 0) {
                    response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/admin-habitaciones.html?error=codigo_existe");
                    return;
                }
                
                // Insertar habitación con todos los campos
                String insertSql = "INSERT INTO habitaciones (codigo, nombre, experiencia, descripcion, precio_noche, capacidad, " +
                                  "metros_cuadrados, wifi, aire_acondicionado, tv, minibar, balcon, vista, camas_info, " +
                                  "desayuno_incluido, cancelacion_gratuita, imagen_principal) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                psInsert = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
                
                psInsert.setString(1, codigo.trim().toUpperCase());
                psInsert.setString(2, nombre.trim());
                psInsert.setString(3, experiencia.trim());
                psInsert.setString(4, descripcion != null ? descripcion.trim() : null);
                psInsert.setDouble(5, precio);
                psInsert.setInt(6, capacidad);
                
                // Nuevos campos
                if (metros != null) {
                    psInsert.setInt(7, metros);
                } else {
                    psInsert.setNull(7, java.sql.Types.INTEGER);
                }
                psInsert.setBoolean(8, wifi);
                psInsert.setBoolean(9, aire);
                psInsert.setBoolean(10, tv);
                psInsert.setBoolean(11, minibar);
                psInsert.setBoolean(12, balcon);
                psInsert.setString(13, vista != null && !vista.isEmpty() ? vista : null);
                psInsert.setString(14, camasInfo != null && !camasInfo.isEmpty() ? camasInfo.trim() : null);
                psInsert.setBoolean(15, desayuno);
                psInsert.setBoolean(16, true); // cancelacion_gratuita por defecto true
                psInsert.setString(17, imagenes.get(0)); // Primera imagen como principal
                
                int filas = psInsert.executeUpdate();
                
                if (filas > 0) {
                    rs = psInsert.getGeneratedKeys();
                    int habitacionId = 0;
                    if (rs.next()) {
                        habitacionId = rs.getInt(1);
                    }
                    
                    // Insertar imágenes
                    String insertImagenSql = "INSERT INTO imagenes_habitacion (habitacion_id, url_cloudinary, orden, es_principal) VALUES (?, ?, ?, ?)";
                    psImagenes = conn.prepareStatement(insertImagenSql);
                    
                    for (int i = 0; i < imagenes.size(); i++) {
                        psImagenes.setInt(1, habitacionId);
                        psImagenes.setString(2, imagenes.get(i));
                        psImagenes.setInt(3, i);
                        psImagenes.setBoolean(4, i == 0);
                        psImagenes.addBatch();
                    }
                    
                    psImagenes.executeBatch();
                    
                    conn.commit();
                    response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/admin-habitaciones.html?exito=guardado");
                } else {
                    conn.rollback();
                    response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/admin-habitaciones.html?error=no_guardado");
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
                response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/admin-habitaciones.html?error=error_sistema");
            } finally {
                DatabaseManager.closeResources(rs, psCheck, psInsert, psImagenes, conn);
            }
            
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/habitacionesCRUD/admin-habitaciones.html?error=precio_invalido");
        }
    }
    
    // Parsear JSON simple de imágenes
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