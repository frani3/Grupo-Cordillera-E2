# Etapa de construcción (Build stage)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar el wrapper de Maven y los archivos de configuración
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x ./mvnw

# Descargar las dependencias (optimización de caché de Docker)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente y compilar
COPY src src
RUN ./mvnw clean package -DskipTests

# Etapa de ejecución (Run stage)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el .jar compilado desde la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto configurado (8081 por defecto en tu yml)
EXPOSE 8081

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]
