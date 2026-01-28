package UX;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Servicio para envío de emails
 * @author Calixto
 */
public class EmailService {
    
    // Configuración del servidor SMTP (Gmail)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "tu-email@gmail.com"; // ⚠️ CAMBIAR ESTO
    private static final String EMAIL_PASSWORD = "tu-app-password"; // ⚠️ CAMBIAR ESTO (App Password de Google)
    
    /**
     * Enviar código de recuperación por email
     */
    public static boolean enviarCodigoRecuperacion(String emailDestino, String codigo) {
        try {
            // Configurar propiedades del servidor SMTP
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            
            // Crear sesión con autenticación
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });
            
            // Crear mensaje
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "Hotel Armonía"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDestino));
            message.setSubject("Código de Recuperación de Contraseña - Hotel Armonía");
            
            // Contenido HTML del email
            String htmlContent = crearHTMLEmail(codigo);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            // Enviar
            Transport.send(message);
            
            System.out.println("✅ Email enviado exitosamente a: " + emailDestino);
            return true;
            
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