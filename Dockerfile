# Stage 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml settings.xml ./
# pre-download deps as a separate cache layer
RUN --mount=type=secret,id=github_token \
    GITHUB_ACTOR=x GITHUB_TOKEN=$(cat /run/secrets/github_token) \
    mvn dependency:go-offline -s settings.xml -q 2>/dev/null || true
COPY src ./src
RUN --mount=type=secret,id=github_token \
    GITHUB_ACTOR=x GITHUB_TOKEN=$(cat /run/secrets/github_token) \
    mvn package -s settings.xml -DskipTests -q

# Stage 2: runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/v2x-tools-backend-1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
