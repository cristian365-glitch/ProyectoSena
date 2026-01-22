# Etapa 1: Compilar
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/ProyectoSena-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
