# Multi-stage build for Spring Boot application - Agent-less version
FROM gradle:8.13-jdk-21-and-24 AS builder

# Set working directory
WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./

# Copy module-specific gradle.properties
COPY internal-core/gradle.properties ./internal-core/

# Copy multi-module source code
COPY app ./app
COPY internal-core ./internal-core

# Build the application
RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Install Node.js, npm, Python, git, openssh and other dependencies
RUN apk add --no-cache \
    bash \
    git \
    openssh-client \
    sudo \
    procps \
    curl

# Reset working directory back to /app
WORKDIR /app

# Create a non-root user
RUN addgroup -S spring && adduser -S -h /home/spring -s /bin/bash -G spring spring

# Create .ssh directory for spring user with proper permissions
RUN mkdir -p /home/spring/.ssh && \
    chmod 700 /home/spring/.ssh && \
    chown -R spring:spring /home/spring/.ssh

# Configure git globally
RUN git config --global push.autoSetupRemote true && \
    git config --global user.email "agent@qodo.ai" && \
    git config --global user.name "Agent User"

# Enable passwordless sudo for spring user
#RUN echo 'spring ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/spring && chmod 0440 /etc/sudoers.d/spring

# Copy the built jar from builder stage (from app module)
COPY --from=builder /app/app/build/libs/command-sdk.jar app.jar

# Copy the agent.yml configuration file to the app directory
# This file will be added to the classpath via the entrypoint script
COPY docker/agent.yml /app/agent.yml

# Change ownership to non-root user
RUN chown -R spring:spring /app


COPY mcp/mcp-internal-1.0.3.jar /app/mcp-internal-1.0.3.jar
RUN chmod +x /app/mcp-internal-1.0.3.jar

# Copy the agentless entrypoint script
COPY docker/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh && \
    chown spring:spring /app/docker-entrypoint.sh


# Switch to non-root user
USER spring

RUN git config --global --add safe.directory '*'

# Add GitHub to known hosts
RUN ssh-keyscan github.com >> /home/spring/.ssh/known_hosts \
 && chmod 600 /home/spring/.ssh/known_hosts

RUN git config --global url."git@github.com:".insteadOf "https://github.com/" \
 && git config --global push.autoSetupRemote true \
 && git config --global user.email "agent@qodo.ai" \
 && git config --global user.name "Agent User"

# Expose the application port
EXPOSE 8081

# Set JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Use the agentless entrypoint script
ENTRYPOINT ["/bin/bash", "/app/docker-entrypoint.sh"]