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
    
    // ⭐ Almacenamiento de tokens (compartido entre métodos)
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
            
            // ⭐ IMPRIMIR CONFIGURACIÓN ANTES DE VALIDAR
            mpConfig.imprimirConfiguracion();
            
            String accessToken = mpConfig.getAccessToken();
            
            // ⭐ VALIDACIÓN MÁS ESTRICTA DEL ACCESS TOKEN
            if (accessToken == null || accessToken.trim().isEmpty()) {
                System.err.println("========================================");
                System.err.println("❌❌❌ CRITICAL ERROR ❌❌❌");
                System.err.println("========================================");
                System.err.println("Access Token NO configurado o vacío");
                System.err.println("");
                System.err.println("SOLUCIONES:");
                System.err.println("1. Configurar variable de entorno MERCADOPAGO_ACCESS_TOKEN");
                System.err.println("2. O configurar en mercadopago.properties:");
                System.err.println("   mercadopago.access.token=APP_USR-XXXX");
                System.err.println("");
                System.err.println("Obtén tu token en:");
                System.err.println("https://www.mercadopago.com.co/developers/panel/credentials");
                System.err.println("========================================");
                throw new ServletException("Mercado Pago Access Token no configurado");
            }
            
            // Validar formato del token
            if (!accessToken.startsWith("APP_USR-") && !accessToken.startsWith("TEST-")) {
                System.err.println("========================================");
                System.err.println("⚠️ ADVERTENCIA: Token con formato inusual");
                System.err.println("========================================");
                System.err.println("Token recibido: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
                System.err.println("Los tokens de MP suelen empezar con APP_USR- o TEST-");
                System.err.println("========================================");
            }
            
            MercadoPagoConfig.setAccessToken(accessToken);
            System.out.println("✅ Mercado Pago SDK configurado exitosamente");
            System.out.println("✅ Modo: " + (mpConfig.isModoTest() ? "TEST (Sandbox)" : "PRODUCCIÓN"));
            System.out.println("✅ Token prefix: " + accessToken.substring(0, Math.min(15, accessToken.length())) + "...");
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ Error crítico en init(): " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            System.err.println("========================================");
            throw new ServletException("Error al inicializar servlet: " + e.getMessage(), e);
        }
        
        System.out.println("========================================");
        System.out.println("✅ Servlet inicializado correctamente");
        System.out.println("========================================");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // ✅ CONFIGURAR RESPONSE COMO JSON ANTES DE CUALQUIER OPERACIÓN
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = null;
        String token = null;
        
        try {
            out = response.getWriter();
            
            // ⭐ VERIFICAR QUE EL SDK ESTÉ CONFIGURADO
            if (mpConfig == null || mpConfig.getAccessToken() == null || mpConfig.getAccessToken().trim().isEmpty()) {
                System.err.println("❌ SDK de Mercado Pago no configurado correctamente");
                enviarError(out, "Error de configuración del sistema de pagos. Contacta al administrador.");
                return;
            }
            
            // ⭐ VERIFICAR SESIÓN
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("logueado") == null) {
                System.err.println("❌ No hay sesión activa");
                enviarError(out, "Debes iniciar sesión para realizar un pago");
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
                enviarError(out, "ID de reserva no especificado");
                return;
            }
            
            int reservaId = Integer.parseInt(reservaIdStr);
            
            // ⭐ PASO 1: GENERAR TOKEN DE SESIÓN
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
            
            // PASO 2: OBTENER DATOS DE LA RESERVA
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = dbManager.getConnection();
                
                String sql = "SELECT r.id, r.total, r.nombre_cliente, r.email, " +
                           "r.num_personas, r.fecha_checkin, r.fecha_checkout, " +
                           "h.nombre as habitacion_nombre, h.codigo as habitacion_codigo " +
                           "FROM reservas r " +
                           "JOIN habitaciones h ON r.habitacion_id = h.id " +
                           "WHERE r.id = ?";
                
                ps = conn.prepareStatement(sql);
                ps.setInt(1, reservaId);
                rs = ps.executeQuery();
                
                if (!rs.next()) {
                    System.err.println("❌ Reserva no encontrada");
                    tokenStorage.remove(token);
                    enviarError(out, "Reserva no encontrada");
                    return;
                }
                
                double total = rs.getDouble("total");
                String nombreCliente = rs.getString("nombre_cliente");
                String email = rs.getString("email");
                String habitacionNombre = rs.getString("habitacion_nombre");
                String habitacionCodigo = rs.getString("habitacion_codigo");
                String fechaCheckin = rs.getString("fecha_checkin");
                String fechaCheckout = rs.getString("fecha_checkout");
                
                System.out.println("✅ Datos de reserva obtenidos:");
                System.out.println("   Cliente: " + nombreCliente);
                System.out.println("   Email: " + email);
                System.out.println("   Total: $" + total);
                System.out.println("   Habitación: " + habitacionNombre);
                
                // VALIDACIONES
                if (email == null || !email.contains("@")) {
                    System.err.println("❌ Email inválido: " + email);
                    tokenStorage.remove(token);
                    enviarError(out, "Email inválido en la reserva");
                    return;
                }
                
                if (total <= 0) {
                    System.err.println("❌ Total inválido: " + total);
                    tokenStorage.remove(token);
                    enviarError(out, "Monto inválido");
                    return;
                }
                
                System.out.println("✅ Validaciones OK");
                
                // PASO 3: CREAR ITEM
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
                
                // ⭐ PASO 4: CONSTRUIR URLs CON TOKEN
                String successUrl = mpConfig.getSuccessUrl(token, String.valueOf(reservaId));
                String failureUrl = mpConfig.getFailureUrl(token, String.valueOf(reservaId));
                String pendingUrl = mpConfig.getPendingUrl(token, String.valueOf(reservaId));
                
                System.out.println("🔗 URLs de retorno configuradas:");
                System.out.println("   ✅ Success: " + successUrl);
                System.out.println("   ❌ Failure: " + failureUrl);
                System.out.println("   ⏳ Pending: " + pendingUrl);
                
                PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                        .success(successUrl)
                        .failure(failureUrl)
                        .pending(pendingUrl)
                        .build();
                
                // PASO 5: CONFIGURAR PAGADOR
                PreferencePayerRequest payer = PreferencePayerRequest.builder()
                        .name(nombreCliente)
                        .email(email)
                        .build();
                
                System.out.println("✅ Pagador configurado");
                
                // PASO 6: CREAR PREFERENCIA
                PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                        .items(items)
                        .backUrls(backUrls)
                        .autoReturn("approved")  // ✅ Auto-return activado
                        .externalReference("RESERVA_" + reservaId)
                        .statementDescriptor("HOTEL ARMONIA")
                        .payer(payer)
                        .notificationUrl(mpConfig.getWebhookUrl())
                        .build();
                
                System.out.println("✅ Request de preferencia construido");
                System.out.println("📤 Enviando solicitud a API de Mercado Pago...");
                
                PreferenceClient client = new PreferenceClient();
                Preference preference = client.create(preferenceRequest);
                
                System.out.println("========================================");
                System.out.println("✅✅✅ PREFERENCIA CREADA EXITOSAMENTE ✅✅✅");
                System.out.println("========================================");
                System.out.println("   Preference ID: " + preference.getId());
                System.out.println("   Init Point: " + preference.getInitPoint());
                System.out.println("   Sandbox Init Point: " + preference.getSandboxInitPoint());
                System.out.println("   Token: " + token);
                System.out.println("========================================");
                
                // Guardar preference ID en BD
                String sqlUpdate = "UPDATE reservas SET mercadopago_preference_id = ? WHERE id = ?";
                PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                psUpdate.setString(1, preference.getId());
                psUpdate.setInt(2, reservaId);
                int updated = psUpdate.executeUpdate();
                psUpdate.close();
                
                System.out.println("✅ Preference ID guardado en BD (filas actualizadas: " + updated + ")");
                
                // ⭐ CONSTRUIR RESPUESTA JSON
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("success", true);
                respuesta.put("preference_id", preference.getId());
                respuesta.put("init_point", mpConfig.isModoTest() ? preference.getSandboxInitPoint() : preference.getInitPoint());
                respuesta.put("sandbox_init_point", preference.getSandboxInitPoint());
                respuesta.put("public_key", mpConfig.getPublicKey());
                respuesta.put("token", token);
                respuesta.put("modo_test", mpConfig.isModoTest());
                
                Gson gson = new Gson();
                String jsonResponse = gson.toJson(respuesta);
                
                out.print(jsonResponse);
                out.flush();
                
                System.out.println("✅ Respuesta JSON enviada al cliente:");
                System.out.println(jsonResponse);
                System.out.println("========================================");
                
            } catch (com.mercadopago.exceptions.MPApiException apiEx) {
                if (token != null) {
                    tokenStorage.remove(token);
                }
                
                System.err.println("========================================");
                System.err.println("❌ ERROR DE API DE MERCADO PAGO");
                System.err.println("========================================");
                System.err.println("   Status Code: " + apiEx.getStatusCode());
                System.err.println("   Message: " + apiEx.getMessage());
                
                try {
                    if (apiEx.getApiResponse() != null) {
                        System.err.println("   API Response: " + apiEx.getApiResponse().getContent());
                    }
                } catch (Exception responseEx) {
                    System.err.println("   (No se pudo obtener respuesta de API)");
                }
                
                apiEx.printStackTrace();
                System.err.println("========================================");
                
                String mensajeUsuario;
                switch (apiEx.getStatusCode()) {
                    case 400:
                        mensajeUsuario = "Datos inválidos para crear el pago. Verifica la información de la reserva.";
                        break;
                    case 401:
                        mensajeUsuario = "Error de autenticación con Mercado Pago. Contacta al administrador (Token inválido).";
                        break;
                    case 403:
                        mensajeUsuario = "Acceso denegado por Mercado Pago. Verifica los permisos de tu aplicación.";
                        break;
                    case 404:
                        mensajeUsuario = "Recurso no encontrado en Mercado Pago.";
                        break;
                    case 429:
                        mensajeUsuario = "Demasiadas solicitudes. Por favor, intenta en unos minutos.";
                        break;
                    default:
                        mensajeUsuario = "Error de Mercado Pago (" + apiEx.getStatusCode() + "). Por favor, intenta nuevamente.";
                        break;
                }
                
                enviarError(out, mensajeUsuario);
                
            } catch (com.mercadopago.exceptions.MPException mpEx) {
                if (token != null) {
                    tokenStorage.remove(token);
                }
                
                System.err.println("========================================");
                System.err.println("❌ ERROR DEL SDK DE MERCADO PAGO");
                System.err.println("========================================");
                System.err.println("   Message: " + mpEx.getMessage());
                mpEx.printStackTrace();
                System.err.println("========================================");
                
                enviarError(out, "Error de comunicación con Mercado Pago. Verifica tu conexión e intenta nuevamente.");
                
            } catch (SQLException e) {
                if (token != null) {
                    tokenStorage.remove(token);
                }
                System.err.println("❌ Error de base de datos: " + e.getMessage());
                e.printStackTrace();
                enviarError(out, "Error de base de datos. Contacta al administrador.");
                
            } finally {
                DatabaseManager.closeResources(rs, ps, conn);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("❌ ID de reserva inválido: " + request.getParameter("reserva_id"));
            e.printStackTrace();
            if (out != null) {
                enviarError(out, "ID de reserva inválido");
            }
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ ERROR INESPERADO EN doPost");
            System.err.println("========================================");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensaje: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
            
            if (out != null) {
                try {
                    enviarError(out, "Error inesperado del servidor. Por favor, intenta nuevamente.");
                } catch (Exception ex) {
                    System.err.println("❌ No se pudo enviar respuesta de error: " + ex.getMessage());
                }
            }
            
        } finally {
            if (out != null) {
                try {
                    out.flush();
                } catch (Exception e) {
                    System.err.println("❌ Error al hacer flush: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * ⭐ MÉTODO GET: VALIDAR Y RESTAURAR TOKEN
     */
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
                System.out.println("🔄 Validando token de sesión");
                System.out.println("   Token: " + token);
                System.out.println("========================================");
                
                if (token == null || token.trim().isEmpty()) {
                    System.err.println("❌ Token no proporcionado");
                    out.print("{\"success\": false, \"error\": \"Token no proporcionado\"}");
                    return;
                }
                
                if (!tokenStorage.containsKey(token)) {
                    System.err.println("❌ Token no existe en storage");
                    out.print("{\"success\": false, \"error\": \"Token inválido o expirado\"}");
                    return;
                }
                
                Map<String, Object> sessionData = tokenStorage.get(token);
                
                // Verificar expiración (1 hora = 3600000 ms)
                long timestamp = (Long) sessionData.get("timestamp");
                long now = System.currentTimeMillis();
                long elapsed = now - timestamp;
                
                if (elapsed > 3600000) {
                    System.err.println("❌ Token expirado");
                    System.err.println("   Tiempo transcurrido: " + (elapsed / 1000) + " segundos");
                    tokenStorage.remove(token);
                    out.print("{\"success\": false, \"error\": \"Token expirado. Por favor, realiza el pago nuevamente.\"}");
                    return;
                }
                
                // ⭐ RESTAURAR SESIÓN
                HttpSession session = request.getSession(true);
                session.setAttribute("logueado", true);
                session.setAttribute("userId", sessionData.get("userId"));
                session.setAttribute("usuario", sessionData.get("usuario"));
                session.setAttribute("email", sessionData.get("email"));
                session.setAttribute("esAdmin", sessionData.get("esAdmin"));
                session.setMaxInactiveInterval(30 * 60); // 30 minutos
                
                // Eliminar token usado (one-time use)
                tokenStorage.remove(token);
                
                System.out.println("✅ Sesión restaurada exitosamente");
                System.out.println("   Usuario: " + sessionData.get("usuario"));
                System.out.println("   Email: " + sessionData.get("email"));
                System.out.println("   Reserva ID: " + sessionData.get("reservaId"));
                System.out.println("   Tiempo de token: " + (elapsed / 1000) + " segundos");
                System.out.println("========================================");
                
                Gson gson = new Gson();
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("success", true);
                respuesta.put("usuario", sessionData.get("usuario"));
                respuesta.put("reservaId", sessionData.get("reservaId"));
                respuesta.put("mensaje", "Sesión restaurada correctamente");
                
                out.print(gson.toJson(respuesta));
                
            } else {
                System.err.println("❌ Acción no válida: " + action);
                out.print("{\"success\": false, \"error\": \"Acción no válida\"}");
            }
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ Error en doGet");
            System.err.println("========================================");
            System.err.println("   Mensaje: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
            out.print("{\"success\": false, \"error\": \"Error del servidor al validar token\"}");
        } finally {
            out.flush();
        }
    }
    
    /**
     * Envía una respuesta de error en formato JSON
     */
    private void enviarError(PrintWriter out, String mensaje) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", mensaje);
            error.put("timestamp", System.currentTimeMillis());
            
            Gson gson = new Gson();
            String jsonError = gson.toJson(error);
            
            out.print(jsonError);
            out.flush();
            
            System.err.println("📤 Respuesta de error enviada:");
            System.err.println("   " + jsonError);
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ ERROR CRÍTICO: No se pudo enviar respuesta de error");
            System.err.println("========================================");
            System.err.println("   Mensaje original: " + mensaje);
            System.err.println("   Error al enviar: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
            
            // Fallback: enviar JSON mínimo
            try {
                out.print("{\"success\":false,\"error\":\"Error del servidor\"}");
                out.flush();
            } catch (Exception ex) {
                System.err.println("❌ Fallback también falló");
            }
        }
    }
    
    @Override
    public void destroy() {
        System.out.println("========================================");
        System.out.println("🛑 Destruyendo CrearPreferenciaMPServlet");
        System.out.println("   Tokens pendientes: " + tokenStorage.size());
        System.out.println("========================================");
        
        tokenStorage.clear();
        super.destroy();
    }
}