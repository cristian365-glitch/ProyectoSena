package com.miapp;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import java.io.File;

public class Main {
    
    public static void main(String[] args) {
        try {
            // Obtener puerto de Render
            String portStr = System.getenv("PORT");
            int port = (portStr != null && !portStr.isEmpty()) 
                       ? Integer.parseInt(portStr) 
                       : 8080;
            
            System.out.println("=================================");
            System.out.println("Iniciando Servidor Tomcat");
            System.out.println("Puerto: " + port);
            System.out.println("=================================");
            
            // Crear instancia de Tomcat
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(port);
            tomcat.getConnector(); // Fuerza la creación del conector
            
            // Buscar directorio webapp en diferentes ubicaciones
            String webappDirLocation = findWebappDirectory();
            System.out.println("Directorio web encontrado: " + webappDirLocation);
            
            // Agregar la aplicación web
            Context context = tomcat.addWebapp("", webappDirLocation);
            
            // Configurar el classloader
            context.setParentClassLoader(Main.class.getClassLoader());
            
            // Agregar recursos adicionales si existen
            File classesDir = new File("target/classes");
            if (classesDir.exists()) {
                StandardRoot resources = new StandardRoot(context);
                resources.addPreResources(
                    new DirResourceSet(resources, "/WEB-INF/classes", 
                    classesDir.getAbsolutePath(), "/")
                );
                context.setResources(resources);
            }
            
            System.out.println("=================================");
            System.out.println("Configuración completada");
            System.out.println("Iniciando servidor...");
            System.out.println("=================================");
            
            // Iniciar Tomcat
            tomcat.start();
            
            System.out.println("=================================");
            System.out.println("✓ Servidor iniciado exitosamente");
            System.out.println("✓ Accede a: http://localhost:" + port);
            System.out.println("✓ Presiona Ctrl+C para detener");
            System.out.println("=================================");
            
            // Mantener el servidor corriendo
            tomcat.getServer().await();
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Error: Puerto inválido");
            e.printStackTrace();
            System.exit(1);
            
        } catch (LifecycleException e) {
            System.err.println("❌ Error al iniciar Tomcat");
            e.printStackTrace();
            System.exit(1);
            
        } catch (Exception e) {
            System.err.println("❌ Error inesperado");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Busca el directorio webapp en diferentes ubicaciones
     */
    private static String findWebappDirectory() {
        // Lista de posibles ubicaciones
        String[] possibleLocations = {
            "/app/webapp",              // Producción en Docker (copiado por Dockerfile)
            "src/main/webapp",          // Desarrollo local
            "web",                      // Alternativa común
            "webapp",                   // Otra alternativa
            "."                         // Directorio actual como último recurso
        };
        
        for (String location : possibleLocations) {
            File dir = new File(location);
            if (dir.exists() && dir.isDirectory()) {
                System.out.println("✓ Encontrado directorio webapp en: " + location);
                return dir.getAbsolutePath();
            }
        }
        
        // Si no se encuentra nada, crear directorio temporal
        System.out.println("⚠ No se encontró directorio webapp, creando temporal");
        File tempDir = new File("temp-webapp");
        tempDir.mkdirs();
        return tempDir.getAbsolutePath();
    }
}