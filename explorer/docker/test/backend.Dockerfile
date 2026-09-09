FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src/main src/main
RUN --mount=type=cache,target=/root/.m2 mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/explorer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
