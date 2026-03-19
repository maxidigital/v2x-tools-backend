FROM eclipse-temurin:21-jre
WORKDIR /app
COPY apps/v2x.tools/target/v2x.tools-1.8-jar-with-dependencies.jar app.jar
COPY apps/v2x.tools/web ./web
ENTRYPOINT ["java", "-jar", "app.jar", "--port", "8080", "--web-enabled", "--debug"]
