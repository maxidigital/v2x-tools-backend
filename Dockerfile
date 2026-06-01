FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/v2x-tools-backend-1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
