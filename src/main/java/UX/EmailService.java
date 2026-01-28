package UX;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Servicio para envío de emails
 * Esta es una clase Java (no un servlet) que proporciona métodos
 * estáticos para el envío de correos electrónicos
 * @author Calixto
 */
public class EmailService {
    
    // ⚠️ CONFIGURACIÓN DEL SERVIDOR SMTP
    // Puedes usar variables de entorno o valores directos
    private static final String SMTP_HOST = System.getenv("SMTP_HOST") != null 
        ? System.getenv("SMTP_HOST") 
        : "smtp.gmail.com";
    
    private static final String SMTP_PORT = System.getenv("SMTP_PORT") != null 
        ? System.getenv("SMTP_PORT") 
        : "587";
    
    private static final String EMAIL_FROM = System.getenv("EMAIL_FROM") != null 
        ? System.getenv("EMAIL_FROM") 
        : "tu-email@gmail.com"; // ⚠️ CAMBIAR
    
    private static final String EMAIL_PASSWORD = System.getenv("EMAIL_PASSWORD") != null 
        ? System.getenv("EMAIL_PASSWORD") 
        : "tu-contraseña-app"; // ⚠️ CAMBIAR (contraseña de aplicación)
    
    private static final String EMAIL_NOMBRE = "Hotel Armonía";
    
    /**
     * Enviar código de recuperación por email
     */
    public static boolean enviarCodigoRecuperacion(String emailDestino, String codigo) {
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });
        
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, EMAIL_NOMBRE));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDestino));
            message.setSubject("Código de Recuperación - Hotel Armonía");
            
            // Contenido HTML del email
            String htmlContent = generarHTMLEmail(codigo);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            // Enviar el email
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
     * Generar HTML del email con estilo
     */
    private static String generarHTMLEmail(String codigo) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "    <meta charset='UTF-8'>" +
               "    <style>" +
               "        body { font-family: 'Arial', sans-serif; background-color: #f5f7fa; margin: 0; padding: 0; }" +
               "        .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }" +
               "        .header { background: linear-gradient(135deg, #d4af37, #b8860b); padding: 30px; text-align: center; }" +
               "        .header h1 { color: white; margin: 0; font-size: 28px; }" +
               "        .content { padding: 40px 30px; }" +
               "        .content h2 { color: #1a1a1a; margin-bottom: 20px; }" +
               "        .content p { color: #666; line-height: 1.6; margin-bottom: 20px; }" +
               "        .codigo-box { background: #f8f9fa; border: 2px dashed #d4af37; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
               "        .codigo { font-size: 36px; font-weight: bold; color: #d4af37; letter-spacing: 8px; }" +
               "        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px; }" +
               "        .warning p { color: #856404; margin: 0; font-size: 14px; }" +
               "        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #999; font-size: 12px; }" +
               "    </style>" +
               "</head>" +
               "<body>" +
               "    <div class='container'>" +
               "        <div class='header'>" +
               "            <h1>🏨 Hotel Armonía</h1>" +
               "        </div>" +
               "        <div class='content'>" +
               "            <h2>Recuperación de Contraseña</h2>" +
               "            <p>Hemos recibido una solicitud para restablecer tu contraseña. Usa el siguiente código de verificación:</p>" +
               "            <div class='codigo-box'>" +
               "                <div class='codigo'>" + codigo + "</div>" +
               "            </div>" +
               "            <p>Este código es válido por <strong>15 minutos</strong>.</p>" +
               "            <div class='warning'>" +
               "                <p>⚠️ Si no solicitaste este cambio, ignora este email. Tu contraseña permanecerá segura.</p>" +
               "            </div>" +
               "        </div>" +
               "        <div class='footer'>" +
               "            <p>© 2026 Hotel Armonía - Todos los derechos reservados</p>" +
               "            <p>Este es un email automático, por favor no respondas a este mensaje.</p>" +
               "        </div>" +
               "    </div>" +
               "</body>" +
               "</html>";
    }
}