package UX;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Servicio de email usando Resend API (funciona perfecto en Render)
 * 3,000 emails/mes GRATIS para siempre - Sin SMTP, usa API REST
 * @author Calixto
 */
public class EmailService {
    
    // 🔧 CONFIGURACIÓN CON VARIABLES DE ENTORNO
    private static final String RESEND_API_KEY;
    private static final String EMAIL_FROM;
    private static final String EMAIL_FROM_NAME = "Hotel Armonía";
    
    // Bloque estático para cargar variables de entorno
    static {
        // Obtener API key desde variables de entorno de Render
        RESEND_API_KEY = System.getenv("RESEND_API_KEY");
        EMAIL_FROM = System.getenv("EMAIL_FROM"); // ej: noreply@tudominio.com
        
        // Verificar que las variables estén configuradas
        if (RESEND_API_KEY == null || RESEND_API_KEY.trim().isEmpty()) {
            System.err.println("❌ ERROR: Variable de entorno RESEND_API_KEY no configurada en Render");
            System.err.println("⚠️ Ve al dashboard de Render → Environment → Add Environment Variable");
        }
        
        if (EMAIL_FROM == null || EMAIL_FROM.trim().isEmpty()) {
            System.err.println("❌ ERROR: Variable de entorno EMAIL_FROM no configurada en Render");
            System.err.println("⚠️ Ejemplo: onboarding@resend.dev (para pruebas) o tu-email@tudominio.com");
        }
        
        if (RESEND_API_KEY != null && EMAIL_FROM != null) {
            System.out.println("✅ Variables de entorno de Resend cargadas correctamente");
            System.out.println("📧 Email configurado: " + EMAIL_FROM);
        }
    }
    
    /**
     * Enviar código de recuperación por email usando Resend API
     */
    public static boolean enviarCodigoRecuperacion(String emailDestino, String codigo) {
        
        // Verificar que las credenciales estén configuradas
        if (RESEND_API_KEY == null || EMAIL_FROM == null) {
            System.err.println("❌ No se puede enviar email: credenciales no configuradas");
            System.err.println("⚠️ Configura RESEND_API_KEY y EMAIL_FROM en Render");
            return false;
        }
        
        try {
            System.out.println("📧 Enviando email a: " + emailDestino);
            System.out.println("📤 Desde: " + EMAIL_FROM);
            
            // Crear la URL de la API de Resend
            URL url = new URL("https://api.resend.com/emails");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Configurar la conexión
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + RESEND_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            // Crear el JSON del email
            JSONObject email = new JSONObject();
            email.put("from", EMAIL_FROM_NAME + " <" + EMAIL_FROM + ">");
            
            // Array de destinatarios
            JSONArray to = new JSONArray();
            to.put(emailDestino);
            email.put("to", to);
            
            email.put("subject", "Código de Recuperación - Hotel Armonía");
            email.put("html", crearHTMLEmail(codigo));
            
            // Enviar la petición
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = email.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Leer la respuesta
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200 || responseCode == 201) {
                // Leer respuesta exitosa
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8")
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                System.out.println("✅ Email enviado exitosamente a: " + emailDestino);
                System.out.println("📬 Respuesta Resend: " + response.toString());
                return true;
            } else {
                // Leer error
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                        conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), 
                        "utf-8"
                    )
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                System.err.println("❌ Error al enviar email. Código HTTP: " + responseCode);
                System.err.println("❌ Respuesta: " + response.toString());
                
                // Mensajes de ayuda según el error
                if (responseCode == 401) {
                    System.err.println("⚠️ SOLUCIÓN: API Key incorrecta. Verifica RESEND_API_KEY en Render");
                } else if (responseCode == 422) {
                    System.err.println("⚠️ SOLUCIÓN: Email FROM incorrecto. Verifica EMAIL_FROM en Render");
                    System.err.println("⚠️ Para pruebas usa: onboarding@resend.dev");
                    System.err.println("⚠️ Para producción verifica tu dominio en Resend");
                }
                
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error al enviar email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Crear contenido HTML del email
     */
    private static String crearHTMLEmail(String codigo) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }" +
                "        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 0 20px rgba(0,0,0,0.1); }" +
                "        .header { background: linear-gradient(135deg, #d4af37, #b8860b); color: white; padding: 30px; text-align: center; }" +
                "        .header h1 { margin: 0; font-size: 28px; }" +
                "        .content { padding: 40px 30px; }" +
                "        .code-box { background: #f8f9fa; border: 2px dashed #d4af37; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
                "        .code { font-size: 36px; font-weight: bold; color: #d4af37; letter-spacing: 8px; font-family: 'Courier New', monospace; }" +
                "        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }" +
                "        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px; }" +
                "        .info { color: #666; font-size: 14px; margin-top: 20px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>🏨 Hotel Armonía</h1>" +
                "            <p style='margin: 5px 0 0 0; font-size: 16px;'>Recuperación de Contraseña</p>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <h2 style='color: #333; margin-top: 0;'>Tu Código de Verificación</h2>" +
                "            <p style='color: #666; line-height: 1.6;'>Hemos recibido una solicitud para restablecer tu contraseña. Usa el siguiente código para continuar:</p>" +
                "            <div class='code-box'>" +
                "                <div class='code'>" + codigo + "</div>" +
                "            </div>" +
                "            <div class='warning'>" +
                "                <strong>⚠️ Importante:</strong> Este código expirará en <strong>15 minutos</strong>. Si no solicitaste este cambio, ignora este mensaje." +
                "            </div>" +
                "            <p class='info'>Gracias por confiar en Hotel Armonía.</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p style='margin: 5px 0;'>&copy; 2024 Hotel Armonía. Todos los derechos reservados.</p>" +
                "            <p style='margin: 5px 0; font-size: 11px;'>Este es un correo automático, por favor no respondas a este mensaje.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Método de prueba para verificar la configuración
     */
    public static boolean verificarConfiguracion() {
        System.out.println("🔍 Verificando configuración de Resend...");
        
        if (RESEND_API_KEY == null || RESEND_API_KEY.trim().isEmpty()) {
            System.err.println("❌ RESEND_API_KEY no configurado");
            return false;
        }
        
        if (EMAIL_FROM == null || EMAIL_FROM.trim().isEmpty()) {
            System.err.println("❌ EMAIL_FROM no configurado");
            return false;
        }
        
        System.out.println("✅ Configuración correcta");
        System.out.println("📧 Email FROM: " + EMAIL_FROM);
        return true;
    }
    
    /**
     * Método main para pruebas
     */
    public static void main(String[] args) {
        System.out.println("🧪 Probando configuración de Resend API...");
        
        if (!verificarConfiguracion()) {
            System.err.println("❌ Configuración incompleta");
            System.err.println("💡 Configura las variables de entorno:");
            System.err.println("   RESEND_API_KEY = re_xxxxxxxxxxxx");
            System.err.println("   EMAIL_FROM = onboarding@resend.dev (para pruebas)");
            return;
        }
        
        // Prueba de envío
        String emailPrueba = "tu-email@ejemplo.com"; // Cambia esto
        String codigoPrueba = "123456";
        
        System.out.println("📧 Enviando email de prueba a: " + emailPrueba);
        boolean resultado = enviarCodigoRecuperacion(emailPrueba, codigoPrueba);
        
        if (resultado) {
            System.out.println("✅ ¡Email enviado! Revisa tu bandeja.");
        } else {
            System.out.println("❌ Error al enviar email. Revisa los logs.");
        }
    }
}