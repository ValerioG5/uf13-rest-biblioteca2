# ============================================
# STAGE 1: BUILD
# ============================================
# Compile the application using Maven and JDK 21
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy Maven configuration and source code
COPY pom.xml .
COPY src ./src

# Build the application
# -DskipTests: Skip tests for faster build
# -q: Quiet mode (less verbose output)
RUN mvn clean package -DskipTests -q

# ============================================
# STAGE 2: RUNTIME
# ============================================
# Minimal JRE Alpine image for production
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create logs directory with proper permissions
RUN mkdir -p /var/log/app && chmod 777 /var/log/app

# Copy only the compiled JAR from build stage
COPY --from=build /app/target/biblioteca-*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Run the application
# SPRING_PROFILES_ACTIVE can be overridden at runtime (e.g., docker run -e SPRING_PROFILES_ACTIVE=prod ...)
ENTRYPOINT ["java", "-jar", "app.jar"]
