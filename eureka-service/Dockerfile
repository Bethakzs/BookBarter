FROM openjdk:21-slim AS build

RUN apt-get update && apt-get install -y maven

COPY . .
RUN mvn clean package

FROM eclipse-temurin:21-jre-jammy

COPY --from=build /target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
