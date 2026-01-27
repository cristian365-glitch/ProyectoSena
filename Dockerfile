# Etapa 1: Compilar con Maven
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml .
COPY src ./src

# Compilar el proyecto
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiar el JAR compilado
COPY --from=build /app/target/ProyectoSena-1.0-SNAPSHOT.jar app.jar

# Copiar la carpeta webapp (HTML, CSS, JS)
COPY --from=build /app/src/main/webapp /app/webapp

# ESTO ES CRÍTICO: Copiar las clases compiladas (donde están tus Servlets)
COPY --from=build /app/target/classes /app/classes

# Exponer puerto
EXPOSE 8080

# Ejecutar la aplicación
CMD ["java", "-jar", "app.jar"]