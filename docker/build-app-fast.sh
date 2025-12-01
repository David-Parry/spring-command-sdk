#!/bin/bash
# Ultra-fast Docker build for development
# Builds JAR locally, then creates minimal Docker image
# Build time: ~30-60 seconds (vs 5-8 minutes for full build)

set -e

# Configuration
APP_IMAGE_NAME="qodo/command-sdk"
APP_IMAGE_TAG=${APP_IMAGE_TAG:-"latest"}
BASE_IMAGE_NAME="qodo/command-sdk-base"
BASE_IMAGE_TAG=${BASE_IMAGE_TAG:-"latest"}

# Determine the correct path based on current directory
if [ -f "Dockerfile.app-fast" ]; then
    # Running from docker/ directory
    DOCKERFILE="Dockerfile.app-fast"
    PROJECT_ROOT=".."
elif [ -f "docker/Dockerfile.app-fast" ]; then
    # Running from project root
    DOCKERFILE="docker/Dockerfile.app-fast"
    PROJECT_ROOT="."
else
    echo -e "${RED}ERROR: Cannot find Dockerfile.app-fast${NC}"
    echo "Please run this script from either:"
    echo "  - Project root: ./docker/build-app-fast.sh"
    echo "  - Docker directory: cd docker && ./build-app-fast.sh"
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Fast Application Build${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if base image exists
if ! docker image inspect "${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}" > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Base image not found: ${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}${NC}"
    echo -e "${YELLOW}Please build the base image first:${NC}"
    echo -e "  ${BLUE}./docker/build-base.sh${NC}"
    echo ""
    exit 1
fi

# Step 1: Build JAR locally
echo -e "${YELLOW}Step 1/3: Building JAR locally...${NC}"
JAR_START=$(date +%s)

# Run gradlew from project root
(cd "$PROJECT_ROOT" && ./gradlew clean bootJar -x test --no-daemon)

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Gradle build failed!${NC}"
    exit 1
fi

JAR_END=$(date +%s)
JAR_DURATION=$((JAR_END - JAR_START))
echo -e "${GREEN}✓ JAR built in ${JAR_DURATION} seconds${NC}"
echo ""

# Verify JAR exists
JAR_PATH="$PROJECT_ROOT/app/build/libs/command-sdk.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}ERROR: JAR not found at $JAR_PATH${NC}"
    exit 1
fi

# Step 2: Prepare Docker build context
echo -e "${YELLOW}Step 2/3: Preparing Docker build context...${NC}"

# Create temporary directory for Docker context
if [ -f "Dockerfile.app-fast" ]; then
    # Running from docker/ directory
    TEMP_DIR="temp-build"
else
    # Running from project root
    TEMP_DIR="docker/temp-build"
fi

mkdir -p "$TEMP_DIR"

# Copy files to Docker context
cp "$PROJECT_ROOT/app/build/libs/command-sdk.jar" "$TEMP_DIR/command-sdk.jar"
cp "$PROJECT_ROOT/docker/agent.yml" "$TEMP_DIR/agent.yml"
cp "$PROJECT_ROOT/mcp/mcp-internal-1.0.3.jar" "$TEMP_DIR/mcp-internal-1.0.3.jar"
cp "$PROJECT_ROOT/docker/docker-entrypoint.sh" "$TEMP_DIR/docker-entrypoint.sh"

echo -e "${GREEN}✓ Build context prepared${NC}"
echo ""

# Step 3: Build Docker image
echo -e "${YELLOW}Step 3/3: Building Docker image...${NC}"
DOCKER_START=$(date +%s)

docker build \
    --file "$DOCKERFILE" \
    --tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" \
    --tag "${APP_IMAGE_NAME}:fast-dev" \
    "$TEMP_DIR"

BUILD_EXIT_CODE=$?
DOCKER_END=$(date +%s)
DOCKER_DURATION=$((DOCKER_END - DOCKER_START))

# Cleanup
rm -rf "$TEMP_DIR"

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}ERROR: Docker build failed!${NC}"
    exit 1
fi

TOTAL_DURATION=$((JAR_DURATION + DOCKER_DURATION))

echo ""
echo -e "${GREEN}✓ Docker image built in ${DOCKER_DURATION} seconds${NC}"
echo ""

# Show image details
echo -e "${BLUE}Image Details:${NC}"
docker images "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Fast Build Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Image: ${GREEN}${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "JAR Build Time: ${GREEN}${JAR_DURATION} seconds${NC}"
echo -e "Docker Build Time: ${GREEN}${DOCKER_DURATION} seconds${NC}"
echo -e "Total Time: ${GREEN}${TOTAL_DURATION} seconds${NC}"
echo ""
echo -e "${BLUE}Performance Comparison:${NC}"
echo -e "  Fast Build: ${GREEN}~${TOTAL_DURATION}s${NC}"
echo -e "  Full Build: ${YELLOW}~300-480s${NC}"
echo -e "  Time Saved: ${GREEN}~$((300 - TOTAL_DURATION))s ($(((300 - TOTAL_DURATION) * 100 / 300))% faster)${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Run the application: ${BLUE}docker run -p 8081:8081 ${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "  2. Or use docker-compose: ${BLUE}docker compose up${NC}"
echo ""
