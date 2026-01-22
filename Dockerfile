# Etapa 1: Compilar con Maven
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app

# Copiar archivos necesarios para compilar
COPY pom.xml .
COPY src ./src
COPY web ./web

# Compilar el proyecto (esto genera el JAR)
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar con Eclipse Temurin
FROM eclipse-temurin:11-jre
WORKDIR /app

# Copiar el JAR compilado
COPY --from=build /app/target/ProyectoSena-1.0-SNAPSHOT.jar app.jar

# IMPORTANTE: Copiar la carpeta web con todos los recursos
# Esta carpeta contiene: index.html, Login.html, auth-session.js, recursos/, nav/, usuario/, etc.
COPY --from=build /app/web /app/web

# Exponer el puerto
EXPOSE 8080

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "app.jar"]