package com.miapp;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.catalina.webresources.DirResourceSet;
import java.io.File;

public class Main {
    
    public static void main(String[] args) throws Exception {
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 8080;
        
        System.out.println("=================================");
        System.out.println("Iniciando Servidor Tomcat");
        System.out.println("Puerto: " + port);
        System.out.println("=================================");
        
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();
        
        // Buscar webapp
        String webappPath = encontrarWebapp();
        
        if (webappPath == null) {
            System.err.println("ERROR: No se encontro webapp");
            System.exit(1);
        }
        
        System.out.println("Webapp encontrado: " + webappPath);
        
        // Crear contexto
        Context ctx = tomcat.addWebapp("", new File(webappPath).getAbsolutePath());
        
        // CLAVE: Agregar el directorio de clases compiladas
        File classesDir = buscarClasses();
        
        if (classesDir != null && classesDir.exists()) {
            System.out.println("Classes encontrado: " + classesDir.getAbsolutePath());
            
            WebResourceRoot resources = new StandardRoot(ctx);
            resources.addPreResources(
                new DirResourceSet(resources, "/WEB-INF/classes", 
                    classesDir.getAbsolutePath(), "/")
            );
            ctx.setResources(resources);
        } else {
            System.out.println("ADVERTENCIA: No se encontro directorio de clases");
        }
        
        // Configurar classloader
        ctx.setParentClassLoader(Main.class.getClassLoader());
        
        tomcat.start();
        
        System.out.println("=================================");
        System.out.println("Servidor iniciado exitosamente");
        System.out.println("URL: http://localhost:" + port);
        System.out.println("=================================");
        
        tomcat.getServer().await();
    }
    
    private static String encontrarWebapp() {
        String[] rutas = {
            "/app/webapp",           // Docker
            "src/main/webapp",       // Local
            "webapp"                 // Alternativa
        };
        
        for (String ruta : rutas) {
            File dir = new File(ruta);
            System.out.println("Buscando webapp en: " + dir.getAbsolutePath());
            
            if (dir.exists() && dir.isDirectory()) {
                File[] archivos = dir.listFiles();
                if (archivos != null && archivos.length > 0) {
                    System.out.println("Encontrado (" + archivos.length + " items)");
                    return dir.getAbsolutePath();
                }
            }
        }
        
        return null;
    }
    
    private static File buscarClasses() {
        String[] rutas = {
            "/app/classes",          // Docker (desde Dockerfile)
            "target/classes",        // Maven local
            "build/classes/java/main" // Gradle
        };
        
        for (String ruta : rutas) {
            File dir = new File(ruta);
            System.out.println("Buscando classes en: " + dir.getAbsolutePath());
            
            if (dir.exists() && dir.isDirectory()) {
                System.out.println("Classes encontrado!");
                return dir;
            }
        }
        
        return null;
    }
}