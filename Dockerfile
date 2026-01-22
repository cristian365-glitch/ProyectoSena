# Etapa 1: Compilar con Maven
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar con Eclipse Temurin
FROM eclipse-temurin:11-jre
WORKDIR /app

# Copiar el JAR compilado
COPY --from=build /app/target/ProyectoSena-1.0-SNAPSHOT.jar app.jar

# NUEVO: Copiar la carpeta webapp con los recursos web
COPY --from=build /app/src/main/webapp /app/webapp

# Exponer el puerto
EXPOSE 8080

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "app.jar"]