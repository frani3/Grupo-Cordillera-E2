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

# Variables de entorno con valores por defecto seguros
ENV SERVER_PORT=8081
ENV DB_HOST=mysql
ENV DB_PORT=3306
ENV DB_NAME=cordillera_pos
ENV DB_USER=root
ENV DB_PASSWORD=root
ENV RABBITMQ_HOST=rabbitmq
ENV RABBITMQ_PORT=5672
ENV RABBITMQ_USER=guest
ENV RABBITMQ_PASSWORD=guest
ENV RABBITMQ_QUEUE_POS=q.pos.deviamserver
ENV ORQUESTADOR_URL=http://orq-service:8080/api/v1/pos

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
