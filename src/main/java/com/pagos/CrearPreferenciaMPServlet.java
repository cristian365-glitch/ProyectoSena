package com.pagos;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.resources.preference.Preference;
import com.google.gson.Gson;
import com.conexiones.DatabaseManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebServlet("/CrearPreferenciaMPServlet")
public class CrearPreferenciaMPServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    private MPConfiguracion mpConfig;
    
    private static Map<String, Map<String, Object>> tokenStorage = new HashMap<>();
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        System.out.println("========================================");
        System.out.println("🚀 Inicializando CrearPreferenciaMPServlet");
        System.out.println("========================================");
        
        try {
            dbManager = DatabaseManager.getInstance();
            mpConfig = MPConfiguracion.getInstance();
            
            if (!dbManager.testConnection()) {
                throw new ServletException("No se puede conectar a la base de datos");
            }
            
            System.out.println("✅ Conexión a BD verificada");
            
            String accessToken = mpConfig.getAccessToken();
            
            if (accessToken == null || accessToken.trim().isEmpty()) {
                System.err.println("❌ MERCADOPAGO_ACCESS_TOKEN no configurada");
                throw new ServletException("MERCADOPAGO_ACCESS_TOKEN no configurada");
            }
            
            MercadoPagoConfig.setAccessToken(accessToken);
            
            System.out.println("✅ Mercado Pago SDK configurado exitosamente");
            System.out.println("✅ Modo: " + (mpConfig.isModoTest() ? "TEST" : "PRODUCCIÓN"));
            System.out.println("✅ Token prefix: " + accessToken.substring(0, Math.min(15, accessToken.length())) + "...");
            
        } catch (Exception e) {
            System.err.println("❌ Error en init(): " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Error al inicializar servlet", e);
        }
        
        System.out.println("========================================");
        System.out.println("✅ Servlet inicializado correctamente");
        System.out.println("========================================");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        PrintWriter out = null;
        String token = null;
        
        try {
            // ⭐ CONFIGURAR RESPONSE INMEDIATAMENTE
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            
            out = response.getWriter();
            
            // Verificar configuración
            if (mpConfig.getAccessToken() == null || mpConfig.getAccessToken().trim().isEmpty()) {
                System.err.println("❌ SDK no configurado");
                enviarError(response, out, "Sistema de pagos no configurado");
                return;
            }
            
            // Verificar sesión
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("logueado") == null) {
                System.err.println("❌ No hay sesión activa");
                enviarError(response, out, "Debes iniciar sesión");
                return;
            }
            
            String reservaIdStr = request.getParameter("reserva_id");
            
            System.out.println("========================================");
            System.out.println("📋 Nueva solicitud de pago");
            System.out.println("   Reserva ID: " + reservaIdStr);
            System.out.println("   Usuario: " + session.getAttribute("usuario"));
            System.out.println("   Timestamp: " + System.currentTimeMillis());
            System.out.println("========================================");
            
            if (reservaIdStr == null || reservaIdStr.trim().isEmpty()) {
                System.err.println("❌ Reserva ID vacío");
                enviarError(response, out, "ID de reserva no especificado");
                return;
            }
            
            int reservaId;
            try {
                reservaId = Integer.parseInt(reservaIdStr);
            } catch (NumberFormatException e) {
                System.err.println("❌ Reserva ID inválido: " + reservaIdStr);
                enviarError(response, out, "ID de reserva inválido");
                return;
            }
            
            // Generar token
            token = UUID.randomUUID().toString();
            
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("userId", session.getAttribute("userId"));
            sessionData.put("usuario", session.getAttribute("usuario"));
            sessionData.put("email", session.getAttribute("email"));
            sessionData.put("esAdmin", session.getAttribute("esAdmin"));
            sessionData.put("reservaId", reservaId);
            sessionData.put("timestamp", System.currentTimeMillis());
            
            tokenStorage.put(token, sessionData);
            
            System.out.println("✅ Token de sesión generado: " + token);
            System.out.println("   Usuario: " + sessionData.get("usuario"));
            System.out.println("   Reserva: " + reservaId);
            
            // Obtener datos de reserva
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                System.out.println("🔄 Obteniendo conexión a BD...");
                conn = dbManager.getConnection();
                System.out.println("✅ Conexión obtenida");
                
                String sql = "SELECT r.id, r.total, r.nombre_cliente, r.email, " +
                           "r.num_personas, r.fecha_checkin, r.fecha_checkout, " +
                           "h.nombre as habitacion_nombre, h.codigo as habitacion_codigo " +
                           "FROM reservas r " +
                           "JOIN habitaciones h ON r.habitacion_id = h.id " +
                           "WHERE r.id = ?";
                
                System.out.println("🔄 Ejecutando query para reserva: " + reservaId);
                ps = conn.prepareStatement(sql);
                ps.setInt(1, reservaId);
                rs = ps.executeQuery();
                
                if (!rs.next()) {
                    System.err.println("❌ Reserva no encontrada en BD");
                    tokenStorage.remove(token);
                    enviarError(response, out, "Reserva no encontrada");
                    return;
                }
                
                double total = rs.getDouble("total");
                String nombreCliente = rs.getString("nombre_cliente");
                String email = rs.getString("email");
                String habitacionNombre = rs.getString("habitacion_nombre");
                String fechaCheckin = rs.getString("fecha_checkin");
                String fechaCheckout = rs.getString("fecha_checkout");
                
                System.out.println("✅ Datos de reserva obtenidos:");
                System.out.println("   Cliente: " + nombreCliente);
                System.out.println("   Email: " + email);
                System.out.println("   Total: $" + total);
                System.out.println("   Habitación: " + habitacionNombre);
                
                // Validaciones
                if (email == null || !email.contains("@")) {
                    System.err.println("❌ Email inválido: " + email);
                    tokenStorage.remove(token);
                    enviarError(response, out, "Email inválido");
                    return;
                }
                
                if (total <= 0) {
                    System.err.println("❌ Total inválido: " + total);
                    tokenStorage.remove(token);
                    enviarError(response, out, "Monto inválido");
                    return;
                }
                
                System.out.println("✅ Validaciones OK");
                
                // Crear item
                System.out.println("🔄 Creando item de Mercado Pago...");
                PreferenceItemRequest item = PreferenceItemRequest.builder()
                        .id("reserva_" + reservaId)
                        .title("Hotel Armonía - " + habitacionNombre)
                        .description(String.format("Reserva del %s al %s", fechaCheckin, fechaCheckout))
                        .categoryId("tourism")
                        .quantity(1)
                        .currencyId("COP")
                        .unitPrice(new BigDecimal(total))
                        .build();
                
                List<PreferenceItemRequest> items = new ArrayList<>();
                items.add(item);
                
                System.out.println("✅ Item creado");
                
                // Construir URLs
                String successUrl = mpConfig.getSuccessUrl(token, String.valueOf(reservaId));
                String failureUrl = mpConfig.getFailureUrl(token, String.valueOf(reservaId));
                String pendingUrl = mpConfig.getPendingUrl(token, String.valueOf(reservaId));
                
                System.out.println("🔗 URLs de retorno:");
                System.out.println("   Success: " + successUrl);
                System.out.println("   Failure: " + failureUrl);
                System.out.println("   Pending: " + pendingUrl);
                
                PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                        .success(successUrl)
                        .failure(failureUrl)
                        .pending(pendingUrl)
                        .build();
                
                // Configurar pagador
                PreferencePayerRequest payer = PreferencePayerRequest.builder()
                        .name(nombreCliente)
                        .email(email)
                        .build();
                
                System.out.println("✅ Pagador configurado");
                
                // Crear preferencia
                System.out.println("🔄 Construyendo request de preferencia...");
                PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                        .items(items)
                        .backUrls(backUrls)
                        .autoReturn("approved")
                        .externalReference("RESERVA_" + reservaId)
                        .statementDescriptor("HOTEL ARMONIA")
                        .payer(payer)
                        .notificationUrl(mpConfig.getWebhookUrl())
                        .build();
                
                System.out.println("✅ Request construido");
                System.out.println("📤 Enviando a API de Mercado Pago...");
                
                PreferenceClient client = new PreferenceClient();
                Preference preference = client.create(preferenceRequest);
                
                System.out.println("========================================");
                System.out.println("✅✅✅ PREFERENCIA CREADA");
                System.out.println("========================================");
                System.out.println("   Preference ID: " + preference.getId());
                System.out.println("   Init Point: " + preference.getInitPoint());
                System.out.println("   Sandbox: " + preference.getSandboxInitPoint());
                System.out.println("========================================");
                
                // Guardar en BD
                String sqlUpdate = "UPDATE reservas SET mercadopago_preference_id = ? WHERE id = ?";
                PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                psUpdate.setString(1, preference.getId());
                psUpdate.setInt(2, reservaId);
                psUpdate.executeUpdate();
                psUpdate.close();
                
                System.out.println("✅ Preference ID guardado en BD");
                
                // Preparar respuesta
                String initPoint = mpConfig.isModoTest() ? 
                    preference.getSandboxInitPoint() : 
                    preference.getInitPoint();
                
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("success", true);
                respuesta.put("preference_id", preference.getId());
                respuesta.put("init_point", initPoint);
                respuesta.put("public_key", mpConfig.getPublicKey());
                respuesta.put("token", token);
                respuesta.put("modo_test", mpConfig.isModoTest());
                
                Gson gson = new Gson();
                String jsonResponse = gson.toJson(respuesta);
                
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(jsonResponse);
                out.flush();
                
                System.out.println("✅ Respuesta enviada:");
                System.out.println("   JSON: " + jsonResponse);
                System.out.println("========================================");
                
            } catch (com.mercadopago.exceptions.MPApiException apiEx) {
                if (token != null) tokenStorage.remove(token);
                
                System.err.println("========================================");
                System.err.println("❌ ERROR API MERCADO PAGO");
                System.err.println("========================================");
                System.err.println("   Status: " + apiEx.getStatusCode());
                System.err.println("   Message: " + apiEx.getMessage());
                
                try {
                    if (apiEx.getApiResponse() != null) {
                        System.err.println("   Response: " + apiEx.getApiResponse().getContent());
                    }
                } catch (Exception e) {
                    // Ignorar
                }
                
                apiEx.printStackTrace();
                System.err.println("========================================");
                
                String mensaje = "Error al crear el pago con Mercado Pago";
                if (apiEx.getStatusCode() == 401) {
                    mensaje = "Error de autenticación con Mercado Pago. Verifica las credenciales.";
                }
                
                enviarError(response, out, mensaje);
                
            } catch (com.mercadopago.exceptions.MPException mpEx) {
                if (token != null) tokenStorage.remove(token);
                
                System.err.println("========================================");
                System.err.println("❌ ERROR SDK MERCADO PAGO");
                System.err.println("========================================");
                mpEx.printStackTrace();
                System.err.println("========================================");
                
                enviarError(response, out, "Error de comunicación con Mercado Pago");
                
            } catch (SQLException e) {
                if (token != null) tokenStorage.remove(token);
                System.err.println("❌ Error BD: " + e.getMessage());
                e.printStackTrace();
                enviarError(response, out, "Error de base de datos");
                
            } finally {
                DatabaseManager.closeResources(rs, ps, conn);
            }
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ ERROR INESPERADO");
            System.err.println("========================================");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensaje: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
            
            if (out != null) {
                try {
                    enviarError(response, out, "Error inesperado del servidor");
                } catch (Exception ex) {
                    System.err.println("❌ No se pudo enviar error: " + ex.getMessage());
                }
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    System.err.println("❌ Error al cerrar writer: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        try {
            String action = request.getParameter("action");
            
            if ("validarToken".equals(action)) {
                String token = request.getParameter("token");
                
                System.out.println("========================================");
                System.out.println("🔄 Validando token: " + token);
                System.out.println("========================================");
                
                if (token == null || !tokenStorage.containsKey(token)) {
                    System.err.println("❌ Token inválido");
                    enviarError(response, out, "Token inválido o expirado");
                    return;
                }
                
                Map<String, Object> sessionData = tokenStorage.get(token);
                
                long timestamp = (Long) sessionData.get("timestamp");
                long elapsed = System.currentTimeMillis() - timestamp;
                
                if (elapsed > 3600000) {
                    System.err.println("❌ Token expirado");
                    tokenStorage.remove(token);
                    enviarError(response, out, "Token expirado");
                    return;
                }
                
                HttpSession session = request.getSession(true);
                session.setAttribute("logueado", true);
                session.setAttribute("userId", sessionData.get("userId"));
                session.setAttribute("usuario", sessionData.get("usuario"));
                session.setAttribute("email", sessionData.get("email"));
                session.setAttribute("esAdmin", sessionData.get("esAdmin"));
                session.setMaxInactiveInterval(30 * 60);
                
                tokenStorage.remove(token);
                
                System.out.println("✅ Sesión restaurada: " + sessionData.get("usuario"));
                System.out.println("========================================");
                
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("success", true);
                respuesta.put("usuario", sessionData.get("usuario"));
                respuesta.put("reservaId", sessionData.get("reservaId"));
                
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(new Gson().toJson(respuesta));
                
            } else {
                enviarError(response, out, "Acción no válida");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en doGet: " + e.getMessage());
            e.printStackTrace();
            enviarError(response, out, "Error del servidor");
        } finally {
            out.flush();
            out.close();
        }
    }
    
    private void enviarError(HttpServletResponse response, PrintWriter out, String mensaje) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", mensaje);
            error.put("timestamp", System.currentTimeMillis());
            
            String jsonError = new Gson().toJson(error);
            
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(jsonError);
            out.flush();
            
            System.err.println("📤 Error enviado: " + jsonError);
            
        } catch (Exception e) {
            System.err.println("❌ Error crítico al enviar respuesta: " + e.getMessage());
            e.printStackTrace();
            try {
                out.print("{\"success\":false,\"error\":\"Error del servidor\"}");
                out.flush();
            } catch (Exception ex) {
                System.err.println("❌ Imposible enviar respuesta");
            }
        }
    }
    
    @Override
    public void destroy() {
        System.out.println("========================================");
        System.out.println("🛑 Destruyendo servlet");
        System.out.println("   Tokens pendientes: " + tokenStorage.size());
        System.out.println("========================================");
        tokenStorage.clear();
        super.destroy();
    }
}