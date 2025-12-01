#!/bin/bash
# Build base Docker image with all system dependencies
# This should be run rarely (only when system dependencies change)

set -e

# Configuration
REGISTRY=${DOCKER_REGISTRY:-""}
BASE_IMAGE_NAME="qodo/command-sdk-base"
BASE_IMAGE_TAG=${BASE_IMAGE_TAG:-"latest"}

# Determine the correct path based on current directory
if [ -f "Dockerfile.base" ]; then
    # Running from docker/ directory
    DOCKERFILE="Dockerfile.base"
    BUILD_CONTEXT=".."
elif [ -f "docker/Dockerfile.base" ]; then
    # Running from project root
    DOCKERFILE="docker/Dockerfile.base"
    BUILD_CONTEXT="."
else
    echo -e "${RED}ERROR: Cannot find Dockerfile.base${NC}"
    echo "Please run this script from either:"
    echo "  - Project root: ./docker/build-base.sh"
    echo "  - Docker directory: cd docker && ./build-base.sh"
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Building Base Docker Image${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Dockerfile exists
if [ ! -f "$DOCKERFILE" ]; then
    echo -e "${RED}ERROR: Dockerfile not found at $DOCKERFILE${NC}"
    exit 1
fi

# Build the base image
echo -e "${YELLOW}Building base image: ${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}${NC}"
echo -e "${YELLOW}Dockerfile: ${DOCKERFILE}${NC}"
echo -e "${YELLOW}Build context: ${BUILD_CONTEXT}${NC}"
echo -e "${YELLOW}This may take 5-10 minutes on first build...${NC}"
echo ""

START_TIME=$(date +%s)

docker build \
    --file "$DOCKERFILE" \
    --tag "${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}" \
    --progress=plain \
    "$BUILD_CONTEXT"

BUILD_EXIT_CODE=$?
END_TIME=$(date +%s)
BUILD_DURATION=$((END_TIME - START_TIME))

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}ERROR: Base image build failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Base image built successfully in ${BUILD_DURATION} seconds${NC}"
echo ""

# Show image details
echo -e "${BLUE}Image Details:${NC}"
docker images "${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
echo ""

# Tag with version if specified
if [ -n "$VERSION" ]; then
    echo -e "${YELLOW}Tagging with version: ${VERSION}${NC}"
    docker tag "${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}" "${BASE_IMAGE_NAME}:${VERSION}"
fi

# Push to registry if specified
if [ -n "$REGISTRY" ]; then
    echo ""
    echo -e "${YELLOW}Pushing base image to registry: ${REGISTRY}${NC}"
    
    FULL_IMAGE_NAME="${REGISTRY}/${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}"
    docker tag "${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}" "$FULL_IMAGE_NAME"
    docker push "$FULL_IMAGE_NAME"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Base image pushed successfully to ${FULL_IMAGE_NAME}${NC}"
    else
        echo -e "${RED}ERROR: Failed to push base image to registry${NC}"
        exit 1
    fi
    
    # Push version tag if specified
    if [ -n "$VERSION" ]; then
        FULL_VERSION_IMAGE="${REGISTRY}/${BASE_IMAGE_NAME}:${VERSION}"
        docker tag "${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}" "$FULL_VERSION_IMAGE"
        docker push "$FULL_VERSION_IMAGE"
    fi
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Base Image Build Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Image: ${GREEN}${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}${NC}"
echo -e "Build Time: ${GREEN}${BUILD_DURATION} seconds${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Build application image: ${BLUE}./docker/build-app.sh${NC}"
echo -e "  2. Or use fast build: ${BLUE}./docker/build-app-fast.sh${NC}"
echo ""
