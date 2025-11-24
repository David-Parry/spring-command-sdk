# syntax=docker/dockerfile:1
# Optimized multi-stage build for Spring Boot application
# This Dockerfile uses layer caching to minimize rebuild times

# ============================================
# Stage 1: Build Stage with Gradle Cache
# ============================================
FROM gradle:8.13-jdk-21-and-24 AS builder

WORKDIR /app

# Copy gradle wrapper and configuration files first (rarely change)
COPY gradle ./gradle
COPY gradlew ./
COPY gradle.properties ./

# Copy build files (change occasionally)
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY internal-core/gradle.properties ./internal-core/

# Download dependencies (cached layer unless build.gradle changes)
# This step is separated to leverage Docker layer caching
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle dependencies --no-daemon || true

# Copy source code (changes frequently)
COPY app ./app
COPY internal-core ./internal-core

# Build the application with cache mount for Gradle
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle clean build -x test --no-daemon

# Verify the JAR was created
RUN ls -lh /app/app/build/libs/ && \
    test -f /app/app/build/libs/command-sdk.jar || (echo "ERROR: JAR not found!" && exit 1)

# ============================================
# Stage 2: Runtime Stage with Base Image
# ============================================
FROM qodo/command-sdk-base:latest

LABEL maintainer="Qodo <support@qodo.ai>"
LABEL description="Qodo Command SDK - AI Agent Orchestration Platform"
LABEL version="1.0.0"

# Switch to root for file operations
USER root

# Copy application artifacts from builder
COPY --from=builder /app/app/build/libs/command-sdk.jar /app/app.jar

# Copy configuration files
COPY docker/agent.yml /app/agent.yml

# Copy MCP internal JAR
COPY mcp/mcp-internal-1.0.3.jar /app/mcp-internal-1.0.3.jar

# Copy entrypoint script
COPY docker/docker-entrypoint.sh /app/docker-entrypoint.sh

# Set proper permissions
RUN chown -R spring:spring /app && \
    chmod +x /app/docker-entrypoint.sh && \
    chmod +x /app/mcp-internal-1.0.3.jar

# Verify files exist
RUN ls -lh /app/ && \
    test -f /app/app.jar || (echo "ERROR: app.jar not found!" && exit 1)

# Switch back to non-root user
USER spring

# Expose application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Use the entrypoint script
ENTRYPOINT ["/bin/bash", "/app/docker-entrypoint.sh"]
