# ==============================================================================
# STAGE 1: Build the Application
# ==============================================================================
# Using a Gradle image with JDK 21
FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

# Copy gradle configuration files first for caching dependencies
COPY build.gradle.kts settings.gradle.kts ./
# Copy source code
COPY src ./src

# Build the application (skip tests for faster build in this stage)
# --no-daemon keeps the environment clean
RUN gradle bootJar --no-daemon -x test

# ==============================================================================
# STAGE 2: Run the Application
# ==============================================================================
# Using a lightweight Java 21 runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Healthcheck configuration
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]