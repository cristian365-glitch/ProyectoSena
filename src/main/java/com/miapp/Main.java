package com.miapp;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import java.io.File;

public class Main {
    
    public static void main(String[] args) throws Exception {
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 8080;
        
        System.out.println("=================================");
        System.out.println("🚀 Iniciando Servidor Tomcat");
        System.out.println("📍 Puerto: " + port);
        System.out.println("=================================");
        
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();
        
        // Buscar webapp
        String webappPath = encontrarWebapp();
        
        if (webappPath == null) {
            System.err.println("❌ No se encontró webapp");
            System.exit(1);
        }
        
        System.out.println("✓ Webapp: " + webappPath);
        
        // Listar archivos para debug
        File webapp = new File(webappPath);
        File[] archivos = webapp.listFiles();
        if (archivos != null) {
            System.out.println("📂 Archivos encontrados:");
            for (File f : archivos) {
                System.out.println("  - " + f.getName());
            }
        }
        
        Context ctx = tomcat.addWebapp("", webappPath);
        ctx.setParentClassLoader(Main.class.getClassLoader());
        
        tomcat.start();
        
        System.out.println("=================================");
        System.out.println("✅ Servidor iniciado");
        System.out.println("🌐 http://localhost:" + port);
        System.out.println("=================================");
        
        tomcat.getServer().await();
    }
    
    private static String encontrarWebapp() {
        String[] rutas = {
            "/app/webapp",           // Docker: copiado desde src/main/webapp
            "src/main/webapp",      // Local: desarrollo
            "webapp"                // Alternativa
        };
        
        for (String ruta : rutas) {
            File dir = new File(ruta);
            System.out.println("🔍 Buscando: " + dir.getAbsolutePath());
            
            if (dir.exists() && dir.isDirectory()) {
                File[] archivos = dir.listFiles();
                if (archivos != null && archivos.length > 0) {
                    System.out.println("✓ Encontrado (" + archivos.length + " items)");
                    return dir.getAbsolutePath();
                }
            }
        }
        
        return null;
    }
}