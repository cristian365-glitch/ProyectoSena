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
            // Obtener puerto de Render (variable de entorno)
            String portStr = System.getenv("PORT");
            int port = (portStr != null && !portStr.isEmpty()) 
                       ? Integer.parseInt(portStr) 
                       : 8080;
            
            System.out.println("=================================");
            System.out.println("🚀 Iniciando Servidor Tomcat");
            System.out.println("📍 Puerto: " + port);
            System.out.println("=================================");
            
            // Crear instancia de Tomcat
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(port);
            tomcat.getConnector(); // Fuerza la creación del conector
            
            // Buscar directorio webapp en diferentes ubicaciones
            String webappPath = buscarDirectorioWebapp();
            
            if (webappPath == null) {
                System.err.println("❌ ERROR: No se encontró el directorio webapp");
                System.err.println("Buscado en:");
                System.err.println("  - /app/web");
                System.err.println("  - web");
                System.err.println("  - src/main/webapp");
                System.exit(1);
            }
            
            System.out.println("✓ Directorio webapp encontrado: " + webappPath);
            
            // Listar contenido del directorio (para debug)
            File webappDir = new File(webappPath);
            System.out.println("📂 Contenido del directorio web:");
            File[] archivos = webappDir.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    System.out.println("  - " + archivo.getName());
                }
            }
            
            // Agregar la aplicación web al contexto raíz ""
            Context context = tomcat.addWebapp("", webappPath);
            
            // Configurar classloader
            context.setParentClassLoader(Main.class.getClassLoader());
            
            // Agregar recursos de clases compiladas si existen
            File classesDir = new File("target/classes");
            if (classesDir.exists()) {
                System.out.println("✓ Agregando clases compiladas");
                StandardRoot resources = new StandardRoot(context);
                resources.addPreResources(
                    new DirResourceSet(resources, "/WEB-INF/classes", 
                    classesDir.getAbsolutePath(), "/")
                );
                context.setResources(resources);
            }
            
            System.out.println("=================================");
            System.out.println("⚙️  Configuración completada");
            System.out.println("🔄 Iniciando servidor...");
            System.out.println("=================================");
            
            // Iniciar Tomcat
            tomcat.start();
            
            System.out.println("=================================");
            System.out.println("✅ Servidor iniciado exitosamente");
            System.out.println("🌐 Accede a: http://localhost:" + port);
            System.out.println("📄 Página principal: http://localhost:" + port + "/index.html");
            System.out.println("🔐 Login: http://localhost:" + port + "/Login.html");
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
     * Orden de prioridad:
     * 1. /app/web (Producción en Docker)
     * 2. web (Desarrollo local)
     * 3. src/main/webapp (Maven estándar)
     */
    private static String buscarDirectorioWebapp() {
        String[] posiblesRutas = {
            "/app/web",           // Producción en Render (Docker)
            "web",                // Desarrollo local (desde raíz del proyecto)
            "src/main/webapp"     // Maven estándar
        };
        
        for (String ruta : posiblesRutas) {
            File dir = new File(ruta);
            
            System.out.println("🔍 Buscando en: " + dir.getAbsolutePath());
            
            if (dir.exists() && dir.isDirectory()) {
                // Verificar que tenga archivos
                File[] archivos = dir.listFiles();
                if (archivos != null && archivos.length > 0) {
                    System.out.println("✓ Directorio encontrado y tiene contenido");
                    return dir.getAbsolutePath();
                } else {
                    System.out.println("⚠ Directorio existe pero está vacío");
                }
            } else {
                System.out.println("✗ Directorio no existe");
            }
        }
        
        return null;
    }
}