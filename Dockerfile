# ================================
# Stage 1: Builder
# ================================
FROM eclipse-temurin:17-jdk-focal AS builder

WORKDIR /app

# Copy maven wrapper and pom
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw package -DskipTests -B

# ================================
# Stage 2: Runtime
# ================================
FROM eclipse-temurin:17-jre-focal

# Security: run as non-root user
RUN groupadd -g 1001 appgroup && 
    useradd -u 1001 -g appgroup -s /bin/sh appuser

WORKDIR /app

# Copy jar from builder
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 
    CMD wget -q --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Startup
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
