# Stage 1: Build app
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run app
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/auth-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=local"]