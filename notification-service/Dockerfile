FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml pom.xml
COPY task-contracts/pom.xml task-contracts/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY task-service/pom.xml task-service/pom.xml
COPY task-contracts/src task-contracts/src
COPY notification-service/src notification-service/src

RUN mvn -pl notification-service -am -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
