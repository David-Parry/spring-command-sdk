#!/bin/bash
# Build application Docker image using optimized Dockerfile
# Uses layer caching for faster builds

set -e

# Configuration
REGISTRY=${DOCKER_REGISTRY:-""}
APP_IMAGE_NAME="qodo/command-sdk"
APP_IMAGE_TAG=${APP_IMAGE_TAG:-"latest"}
BASE_IMAGE_NAME="qodo/command-sdk-base"
BASE_IMAGE_TAG=${BASE_IMAGE_TAG:-"latest"}

# Determine the correct path based on current directory
if [ -f "Dockerfile.app" ]; then
    # Running from docker/ directory
    DOCKERFILE="Dockerfile.app"
    BUILD_CONTEXT=".."
elif [ -f "docker/Dockerfile.app" ]; then
    # Running from project root
    DOCKERFILE="docker/Dockerfile.app"
    BUILD_CONTEXT="."
else
    echo -e "${RED}ERROR: Cannot find Dockerfile.app${NC}"
    echo "Please run this script from either:"
    echo "  - Project root: ./docker/build-app.sh"
    echo "  - Docker directory: cd docker && ./build-app.sh"
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Building Application Docker Image${NC}"
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

# Check if Dockerfile exists
if [ ! -f "$DOCKERFILE" ]; then
    echo -e "${RED}ERROR: Dockerfile not found at $DOCKERFILE${NC}"
    exit 1
fi

# Enable BuildKit for better caching
export DOCKER_BUILDKIT=1

echo -e "${YELLOW}Building application image: ${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "${YELLOW}Using base image: ${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}${NC}"
echo -e "${YELLOW}Dockerfile: ${DOCKERFILE}${NC}"
echo -e "${YELLOW}Build context: ${BUILD_CONTEXT}${NC}"
echo ""

START_TIME=$(date +%s)

# Build with BuildKit cache
docker build \
    --file "$DOCKERFILE" \
    --tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    --progress=plain \
    "$BUILD_CONTEXT"

BUILD_EXIT_CODE=$?
END_TIME=$(date +%s)
BUILD_DURATION=$((END_TIME - START_TIME))

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}ERROR: Application image build failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Application image built successfully in ${BUILD_DURATION} seconds${NC}"
echo ""

# Show image details
echo -e "${BLUE}Image Details:${NC}"
docker images "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
echo ""

# Tag with version if specified
if [ -n "$VERSION" ]; then
    echo -e "${YELLOW}Tagging with version: ${VERSION}${NC}"
    docker tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" "${APP_IMAGE_NAME}:${VERSION}"
fi

# Push to registry if specified
if [ -n "$REGISTRY" ]; then
    echo ""
    echo -e "${YELLOW}Pushing application image to registry: ${REGISTRY}${NC}"
    
    FULL_IMAGE_NAME="${REGISTRY}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"
    docker tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" "$FULL_IMAGE_NAME"
    docker push "$FULL_IMAGE_NAME"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Application image pushed successfully to ${FULL_IMAGE_NAME}${NC}"
    else
        echo -e "${RED}ERROR: Failed to push application image to registry${NC}"
        exit 1
    fi
    
    # Push version tag if specified
    if [ -n "$VERSION" ]; then
        FULL_VERSION_IMAGE="${REGISTRY}/${APP_IMAGE_NAME}:${VERSION}"
        docker tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" "$FULL_VERSION_IMAGE"
        docker push "$FULL_VERSION_IMAGE"
    fi
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Application Image Build Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Image: ${GREEN}${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "Build Time: ${GREEN}${BUILD_DURATION} seconds${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Run the application: ${BLUE}docker run -p 8081:8081 ${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "  2. Or use docker compose: ${BLUE}docker compose up${NC}"
echo -e "  3. Or use docker compose with logfile: ${BLUE}docker compose up 2>&1 | tee out.log${NC}"
echo ""
