# --- Etapa 1: build (compila el jar con Maven dentro de la imagen) ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Etapa 2: runtime (solo el JRE y el jar) ---
FROM eclipse-temurin:17-jre
WORKDIR /app
# Punto de montaje del EFS dentro del contenedor (se inyecta con -v /mnt/efs:/app/efs)
RUN mkdir -p /app/efs
ENV EFS_PATH=/app/efs
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
