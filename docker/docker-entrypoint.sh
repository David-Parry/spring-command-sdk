#!/bin/bash

# Ensure PATH includes common binary locations
export PATH="/usr/local/bin:/usr/bin:/bin:$PATH"

echo "Starting Spring Boot application..."
echo "========================================="
echo "PATH: $PATH"
echo "Snyk location: $(which snyk || echo 'not found')"
echo ""

# Start Spring Boot application
exec java $JAVA_OPTS -jar app.jar