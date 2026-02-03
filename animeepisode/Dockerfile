# ========================================
# STAGE 1: Build
# ========================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml trước để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster build)
RUN mvn clean package -DskipTests

# ========================================
# STAGE 2: Runtime
# ========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR từ build stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]