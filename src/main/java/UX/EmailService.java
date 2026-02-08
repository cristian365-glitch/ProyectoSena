package UX;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Servicio de email usando Gmail SMTP con variables de entorno de Render
 * 100% GRATIS - Configurado para producción en Render
 * @author Calixto
 */
public class EmailService {
    
    // 🔧 CONFIGURACIÓN CON VARIABLES DE ENTORNO
    // Estas variables se configuran en Render.com en el dashboard
    private static final String GMAIL_USERNAME;
    private static final String GMAIL_APP_PASSWORD;
    private static final String EMAIL_FROM_NAME = "Hotel Armonía";
    
    // Bloque estático para cargar variables de entorno
    static {
        // Obtener credenciales desde variables de entorno de Render
        GMAIL_USERNAME = System.getenv("GMAIL_USERNAME");
        GMAIL_APP_PASSWORD = System.getenv("GMAIL_APP_PASSWORD");
        
        // Verificar que las variables estén configuradas
        if (GMAIL_USERNAME == null || GMAIL_USERNAME.trim().isEmpty()) {
            System.err.println("❌ ERROR: Variable de entorno GMAIL_USERNAME no configurada en Render");
            System.err.println("⚠️ Ve al dashboard de Render → Environment → Add Environment Variable");
        }
        
        if (GMAIL_APP_PASSWORD == null || GMAIL_APP_PASSWORD.trim().isEmpty()) {
            System.err.println("❌ ERROR: Variable de entorno GMAIL_APP_PASSWORD no configurada en Render");
            System.err.println("⚠️ Ve al dashboard de Render → Environment → Add Environment Variable");
        }
        
        if (GMAIL_USERNAME != null && GMAIL_APP_PASSWORD != null) {
            System.out.println("✅ Variables de entorno de Gmail cargadas correctamente");
            System.out.println("📧 Email configurado: " + GMAIL_USERNAME);
        }
    }
    
    /**
     * Enviar código de recuperación por email usando Gmail SMTP
     */
    public static boolean enviarCodigoRecuperacion(String emailDestino, String codigo) {
        
        // Verificar que las credenciales estén configuradas
        if (GMAIL_USERNAME == null || GMAIL_APP_PASSWORD == null) {
            System.err.println("❌ No se puede enviar email: credenciales no configuradas");
            System.err.println("⚠️ Configura GMAIL_USERNAME y GMAIL_APP_PASSWORD en Render");
            return false;
        }
        
        try {
            System.out.println("📧 Enviando email a: " + emailDestino);
            System.out.println("📤 Desde: " + GMAIL_USERNAME);
            
            // Configurar propiedades de Gmail SMTP
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            // Configuración adicional para Render
            props.put("mail.smtp.connectiontimeout", "10000"); // 10 segundos
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");
            
            // Crear sesión con autenticación
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_APP_PASSWORD);
                }
            });
            
            // Habilitar debug solo si hay problemas (comentar en producción)
            // session.setDebug(true);
            
            // Crear el mensaje
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(GMAIL_USERNAME, EMAIL_FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDestino));
            message.setSubject("Código de Recuperación - Hotel Armonía");
            
            // Contenido HTML
            message.setContent(crearHTMLEmail(codigo), "text/html; charset=utf-8");
            
            // Enviar
            Transport.send(message);
            
            System.out.println("✅ Email enviado exitosamente a: " + emailDestino);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("❌ Error al enviar email: " + e.getMessage());
            e.printStackTrace();
            
            // Mostrar ayuda según el error
            if (e.getMessage().contains("Username and Password not accepted")) {
                System.err.println("⚠️ SOLUCIÓN: Verifica la contraseña de aplicación en Render");
                System.err.println("⚠️ Debe ser una 'Contraseña de aplicación' de 16 caracteres");
            } else if (e.getMessage().contains("Could not connect")) {
                System.err.println("⚠️ SOLUCIÓN: Problema de conexión desde Render");
                System.err.println("⚠️ Verifica que Render tenga acceso a internet");
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
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
     * Se puede llamar desde RecuperarPasswordServlet al inicializar
     */
    public static boolean verificarConfiguracion() {
        System.out.println("🔍 Verificando configuración de email...");
        
        if (GMAIL_USERNAME == null || GMAIL_USERNAME.trim().isEmpty()) {
            System.err.println("❌ GMAIL_USERNAME no configurado");
            return false;
        }
        
        if (GMAIL_APP_PASSWORD == null || GMAIL_APP_PASSWORD.trim().isEmpty()) {
            System.err.println("❌ GMAIL_APP_PASSWORD no configurado");
            return false;
        }
        
        System.out.println("✅ Configuración correcta");
        System.out.println("📧 Email: " + GMAIL_USERNAME);
        return true;
    }
    
    /**
     * Método main para pruebas locales
     * En producción esto no se ejecuta
     */
    public static void main(String[] args) {
        System.out.println("🧪 Probando configuración de Gmail SMTP...");
        System.out.println("📍 Entorno: " + (GMAIL_USERNAME != null ? "Render/Producción" : "Local"));
        
        if (!verificarConfiguracion()) {
            System.err.println("❌ Configuración incompleta");
            System.err.println("💡 Para pruebas locales, configura las variables de entorno:");
            System.err.println("   export GMAIL_USERNAME='tu-email@gmail.com'");
            System.err.println("   export GMAIL_APP_PASSWORD='xxxx xxxx xxxx xxxx'");
            return;
        }
        
        // Prueba de envío (solo si las variables están configuradas)
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