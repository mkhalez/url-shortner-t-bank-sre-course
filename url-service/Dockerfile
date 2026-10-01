FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./pom.xml
COPY url-service/pom.xml ./url-service/pom.xml

WORKDIR /build/url-service
RUN mvn dependency:go-offline -B

COPY url-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /url_service

COPY --from=builder /build/url-service/target/url-service-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "app.jar"]