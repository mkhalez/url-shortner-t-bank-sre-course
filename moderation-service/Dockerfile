FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./pom.xml
COPY moderation-service/pom.xml ./moderation-service/pom.xml

WORKDIR /build/moderation-service
RUN mvn dependency:go-offline -B

COPY moderation-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /moderation_service

COPY --from=builder /build/moderation-service/target/moderation-service-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "app.jar"]