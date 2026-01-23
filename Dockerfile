# Etapa 1: Compilar con Maven
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml .
COPY src ./src

# Compilar (Maven automáticamente empaqueta src/main/webapp/ en el JAR)
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar
FROM eclipse-temurin:11-jre
WORKDIR /app

# Copiar el JAR (ya contiene webapp/)
COPY --from=build /app/target/ProyectoSena-1.0-SNAPSHOT.jar app.jar

# Copiar webapp por separado para acceso directo
COPY --from=build /app/src/main/webapp /app/webapp

# Exponer puerto
EXPOSE 8080

# Ejecutar
CMD ["java", "-jar", "app.jar"]