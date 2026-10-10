# Portfolio Pilot backend — production image.
# Multi-stage: Maven + Eclipse Temurin 21 builds, slim JRE 21 runs.
# No secrets baked in: everything arrives via Render environment variables.
# The app reads Render's PORT itself (server.port=${PORT:8080}).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/portfolio-pilot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
