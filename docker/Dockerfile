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

# Runtime stage - Using Ubuntu-based image with OpenJDK and Node.js for better compatibility
FROM timbru31/java-node:21-jdk-20

# Set working directory
WORKDIR /app

# Install other dependenciesHost github.com
    HostName github.com
    User git
    IdentityFile /home/spring/.ssh/aws_ecdsa
    IdentitiesOnly yes
    StrictHostKeyChecking accept-new
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    bash \
    git \
    openssh-client \
    sudo \
    procps \
    curl \
    ca-certificates \
    gnupg \
    maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Reset working directory back to /app
WORKDIR /app

# Create a non-root user (Ubuntu syntax)
RUN groupadd -r spring && useradd -r -g spring -d /home/spring -s /bin/bash -m spring

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

# Install Snyk CLI globally using npm (works on all architectures)
# This is the recommended installation method and supports both x86_64 and ARM64
RUN npm install -g snyk@latest && \
    npm cache clean --force

# Verify installations
RUN node --version && \
    npm --version && \
    snyk --version && \
    mvn --version

# Set PATH environment variable to ensure binaries are found
ENV PATH="/usr/local/bin:/usr/bin:/bin:${PATH}"

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