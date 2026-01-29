package UX;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/**
 * Servicio de email usando SendGrid API
 * @author Calixto
 */
public class EmailService {
    
    // ⚠️ CONFIGURAR ESTO CON TU API KEY DE SENDGRID
    private static final String SENDGRID_API_KEY = "TU_SENDGRID_API_KEY_AQUI";
    private static final String EMAIL_FROM = "noreply@tuhotel.com"; // El email verificado en SendGrid
    private static final String EMAIL_FROM_NAME = "Hotel Armonía";
    
    /**
     * Enviar código de recuperación por email usando SendGrid
     */
    public static boolean enviarCodigoRecuperacion(String emailDestino, String codigo) {
        try {
            // Crear JSON para SendGrid API v3
            JSONObject email = new JSONObject();
            
            // Personalización (from)
            JSONObject from = new JSONObject();
            from.put("email", EMAIL_FROM);
            from.put("name", EMAIL_FROM_NAME);
            email.put("from", from);
            
            // Destinatario (to)
            JSONObject to = new JSONObject();
            to.put("email", emailDestino);
            email.put("personalizations", new org.json.JSONArray().put(
                new JSONObject().put("to", new org.json.JSONArray().put(to))
            ));
            
            // Asunto
            email.put("subject", "Código de Recuperación - Hotel Armonía");
            
            // Contenido HTML
            JSONObject content = new JSONObject();
            content.put("type", "text/html");
            content.put("value", crearHTMLEmail(codigo));
            email.put("content", new org.json.JSONArray().put(content));
            
            // Hacer petición a SendGrid
            URL url = new URL("https://api.sendgrid.com/v3/mail/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + SENDGRID_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            // Enviar
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = email.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Verificar respuesta
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 202) { // SendGrid retorna 202 Accepted
                System.out.println("✅ Email enviado exitosamente a: " + emailDestino);
                return true;
            } else {
                // Leer error
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.err.println("❌ Error al enviar email. Código: " + responseCode);
                System.err.println("❌ Respuesta: " + response.toString());
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
                "        .code { font-size: 36px; font-weight: bold; color: #d4af37; letter-spacing: 8px; }" +
                "        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }" +
                "        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>🏨 Hotel Armonía</h1>" +
                "            <p>Recuperación de Contraseña</p>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <h2>Tu Código de Verificación</h2>" +
                "            <p>Hemos recibido una solicitud para restablecer tu contraseña. Usa el siguiente código para continuar:</p>" +
                "            <div class='code-box'>" +
                "                <div class='code'>" + codigo + "</div>" +
                "            </div>" +
                "            <div class='warning'>" +
                "                <strong>⚠️ Importante:</strong> Este código expirará en 15 minutos. Si no solicitaste este cambio, ignora este mensaje." +
                "            </div>" +
                "            <p>Gracias por confiar en Hotel Armonía.</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>&copy; 2024 Hotel Armonía. Todos los derechos reservados.</p>" +
                "            <p>Este es un correo automático, por favor no respondas a este mensaje.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}