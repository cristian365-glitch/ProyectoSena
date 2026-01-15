package com.pagos;

import com.conexiones.DatabaseManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;

@WebServlet("/webhook/mercadopago")
public class WebhookMPServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DatabaseManager dbManager;
    private MPConfiguracion mpConfig;
    
    @Override
    public void init() throws ServletException {
        super.init();
        dbManager = DatabaseManager.getInstance();
        mpConfig = MPConfiguracion.getInstance();
        
        String accessToken = mpConfig.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
            MercadoPagoConfig.setAccessToken(accessToken);
        }
        
        System.out.println("✅ Webhook de Mercado Pago inicializado");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        System.out.println("========================================");
        System.out.println("📩 WEBHOOK RECIBIDO DE MERCADO PAGO");
        System.out.println("========================================");
        
        // Obtener headers para validación
        String xSignature = request.getHeader("x-signature");
        String xRequestId = request.getHeader("x-request-id");
        
        System.out.println("🔐 Headers de seguridad:");
        System.out.println("   x-signature: " + xSignature);
        System.out.println("   x-request-id: " + xRequestId);
        
        // Leer el body
        String body = request.getReader().lines().collect(Collectors.joining("\n"));
        System.out.println("📦 Body recibido: " + body);
        
        // Validar firma (opcional pero recomendado)
        if (xSignature != null && !validarFirma(body, xSignature, xRequestId)) {
            System.err.println("❌ Firma inválida - Posible ataque");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("Invalid signature");
            return;
        }
        
        System.out.println("✅ Firma validada correctamente");
        
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            
            String action = json.has("action") ? json.get("action").getAsString() : "";
            String type = json.has("type") ? json.get("type").getAsString() : "";
            
            System.out.println("🔔 Tipo: " + type);
            System.out.println("🔔 Acción: " + action);
            
            if ("payment".equals(type)) {
                JsonObject data = json.getAsJsonObject("data");
                String paymentId = data.get("id").getAsString();
                
                System.out.println("💰 Payment ID: " + paymentId);
                System.out.println("🔄 Consultando detalles del pago...");
                
                try {
                    PaymentClient paymentClient = new PaymentClient();
                    Payment payment = paymentClient.get(Long.parseLong(paymentId));
                    
                    System.out.println("========================================");
                    System.out.println("💳 DETALLES DEL PAGO");
                    System.out.println("========================================");
                    System.out.println("   ID: " + payment.getId());
                    System.out.println("   Status: " + payment.getStatus());
                    System.out.println("   Status Detail: " + payment.getStatusDetail());
                    System.out.println("   Amount: $" + payment.getTransactionAmount());
                    System.out.println("   External Ref: " + payment.getExternalReference());
                    System.out.println("   Payer Email: " + payment.getPayer().getEmail());
                    System.out.println("========================================");
                    
                    String externalReference = payment.getExternalReference();
                    if (externalReference != null && externalReference.startsWith("RESERVA_")) {
                        int reservaId = Integer.parseInt(externalReference.replace("RESERVA_", ""));
                        
                        System.out.println("📋 Actualizando Reserva #" + reservaId);
                        actualizarEstadoReserva(reservaId, payment);
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ Error al obtener detalles del pago");
                    e.printStackTrace();
                }
            }
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("OK");
            
            System.out.println("✅ Webhook procesado correctamente");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("❌ Error al procesar webhook");
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("Webhook activo");
    }
    
    /**
     * Valida la firma del webhook de Mercado Pago
     */
    private boolean validarFirma(String body, String xSignature, String xRequestId) {
        try {
            String secret = mpConfig.getWebhookSecret();
            
            if (secret == null || secret.isEmpty()) {
                System.out.println("⚠️ Webhook secret no configurado - saltando validación");
                return true; // Si no hay secret configurado, permitir (solo para desarrollo)
            }
            
            // Parsear x-signature
            // Formato: ts=timestamp,v1=hash
            String[] parts = xSignature.split(",");
            String ts = null;
            String hash = null;
            
            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue.length == 2) {
                    if ("ts".equals(keyValue[0])) {
                        ts = keyValue[1];
                    } else if ("v1".equals(keyValue[0])) {
                        hash = keyValue[1];
                    }
                }
            }
            
            if (ts == null || hash == null) {
                System.err.println("⚠️ Formato de firma inválido");
                return false;
            }
            
            // Construir el manifest
            String manifest = "id:" + xRequestId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
            
            // Calcular HMAC SHA256
            String dataToSign = manifest + body;
            String calculatedHash = calcularHMACSHA256(secret, dataToSign);
            
            boolean esValido = hash.equals(calculatedHash);
            
            if (!esValido) {
                System.err.println("❌ Firma no coincide");
                System.err.println("   Esperada: " + hash);
                System.err.println("   Calculada: " + calculatedHash);
            }
            
            return esValido;
            
        } catch (Exception e) {
            System.err.println("❌ Error al validar firma");
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Calcula HMAC SHA256
     */
    private String calcularHMACSHA256(String secret, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = 
                new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
            return DatatypeConverter.printHexBinary(hash).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando HMAC", e);
        }
    }
    
    /**
     * Actualiza el estado de la reserva
     */
    private void actualizarEstadoReserva(int reservaId, Payment payment) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = dbManager.getConnection();
            
            String nuevoEstado = null;
            String paymentStatus = payment.getStatus();
            
            switch (paymentStatus) {
                case "approved":
                    nuevoEstado = "confirmada";
                    System.out.println("✅ Pago APROBADO");
                    break;
                case "pending":
                case "in_process":
                    nuevoEstado = "pendiente_pago";
                    System.out.println("⏳ Pago PENDIENTE");
                    break;
                case "rejected":
                case "cancelled":
                    nuevoEstado = "cancelada";
                    System.out.println("❌ Pago RECHAZADO");
                    break;
                default:
                    System.out.println("⚠️ Estado desconocido: " + paymentStatus);
                    break;
            }
            
            if (nuevoEstado != null) {
                String sql = "UPDATE reservas SET " +
                           "estado = ?, " +
                           "mercadopago_payment_id = ?, " +
                           "mercadopago_payment_status = ? " +
                           "WHERE id = ?";
                
                ps = conn.prepareStatement(sql);
                ps.setString(1, nuevoEstado);
                ps.setString(2, payment.getId().toString());
                ps.setString(3, paymentStatus);
                ps.setInt(4, reservaId);
                
                int filasActualizadas = ps.executeUpdate();
                
                if (filasActualizadas > 0) {
                    System.out.println("========================================");
                    System.out.println("✅ RESERVA ACTUALIZADA");
                    System.out.println("========================================");
                    System.out.println("   Reserva: #" + reservaId);
                    System.out.println("   Estado: " + nuevoEstado);
                    System.out.println("   Payment ID: " + payment.getId());
                    System.out.println("========================================");
                } else {
                    System.err.println("⚠️ Reserva #" + reservaId + " no encontrada");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error al actualizar reserva");
            e.printStackTrace();
        } finally {
            DatabaseManager.closeResources(null, ps, conn);
        }
    }
}