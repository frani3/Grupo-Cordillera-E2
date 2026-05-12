## ── Etapa 1: compilación ──────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Descarga dependencias primero (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

## ── Etapa 2: imagen de ejecución ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Variables de entorno con valores por defecto seguros
ENV SERVER_PORT=8080
ENV ORQ_SERVICE_URL=http://orq-service:8080

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
