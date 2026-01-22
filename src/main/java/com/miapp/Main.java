package com.miapp;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import java.io.File;

public class Main {
    
    public static void main(String[] args) throws Exception {
        // Render proporciona el puerto en la variable PORT
        String port = System.getenv("PORT");
        if (port == null || port.isEmpty()) {
            port = "8080";
        }
        
        System.out.println("Iniciando en puerto: " + port);
        
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(Integer.parseInt(port));
        
        // Configurar webapp
        String contextPath = "";
        String docBase = new File("src/main/webapp").getAbsolutePath();
        
        Context context = tomcat.addWebapp(contextPath, docBase);
        context.setParentClassLoader(Main.class.getClassLoader());
        
        tomcat.start();
        System.out.println("Servidor iniciado: http://localhost:" + port);
        tomcat.getServer().await();
    }
}