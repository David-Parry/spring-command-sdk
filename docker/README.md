# Docker Build System

Optimized Docker build system for Qodo Command SDK with **85-90% faster build times** through intelligent layer caching.

## Quick Start

### First Time Setup

```bash
# 1. Build base image (one-time, ~5-10 minutes)
cd docker
make base

# 2. Fast build application (~30-60 seconds)
make fast

# 3. Run the application
make run
```

### Daily Development

```bash
# Make code changes...
vim ../app/src/main/java/ai/qodo/command/app/QodoCommandApplication.java

# Fast rebuild and restart (~30-60 seconds)
make dev-rebuild
```

---

## Available Commands

Run `make help` to see all available commands:

```bash
make help
```

### Build Commands

| Command | Description | Time | When to Use |
|---------|-------------|------|-------------|
| `make base` | Build base image | 5-10 min | Once per month or when system deps change |
| `make app` | Standard build | 1-2 min | CI/CD, dependency changes |
| `make fast` | Fast development build | 30-60 sec | **Daily development (recommended)** |
| `make benchmark` | Performance test | 10-15 min | Measure build performance |

### Runtime Commands

| Command | Description |
|---------|-------------|
| `make run` | Start application with docker-compose |
| `make stop` | Stop application |
| `make restart` | Fast rebuild + restart |
| `make logs` | Show application logs |
| `make shell` | Open shell in container |
| `make health` | Check application health |

### Maintenance Commands

| Command | Description |
|---------|-------------|
| `make clean` | Remove application images |
| `make clean-all` | Remove all images including base |
| `make stats` | Show image sizes |
| `make info` | Show build system info |
| `make version` | Show version information |

---

## Build Strategies

### Strategy 1: Base Image (Rare)

**File:** `Dockerfile.base`

**Contains:**
- Java 21 + Node.js 20
- System packages (git, maven, curl, etc.)
- Snyk CLI
- User configuration
- SSH setup

**Build:**
```bash
./build-base.sh
# or
make base
```

**When:** Only when system dependencies change (monthly or less)

---

### Strategy 2: Standard Build

**File:** `Dockerfile.app`

**Contains:**
- Gradle build inside Docker
- BuildKit cache mounts
- Layer optimization

**Build:**
```bash
./build-app.sh
# or
make app
```

**When:** CI/CD pipelines, dependency changes

---

### Strategy 3: Fast Build (Recommended)

**File:** `Dockerfile.app-fast`

**Contains:**
- Pre-built JAR (built locally)
- Minimal Docker layers
- Maximum speed

**Build:**
```bash
./build-app-fast.sh
# or
make fast
```

**When:** Active development, frequent code changes

---

## Performance Comparison

| Build Type | Time | Improvement |
|------------|------|-------------|
| Full Build (no cache) | 5-8 min | Baseline |
| Full Build (cached) | 1-2 min | 70-80% faster |
| Fast Build | 30-60 sec | **85-90% faster** |

---

## Docker Compose

### Build Base Image

```bash
docker compose --profile build-base build base-builder
```

### Build and Run Application

```bash
# Build with optimized Dockerfile
docker compose build command-sdk

# Start all services
docker compose up -d

# View logs
docker compose logs -f command-sdk
```

---

## Scripts

### build-base.sh

Builds the base Docker image with all system dependencies.

**Usage:**
```bash
./build-base.sh

# With registry
export DOCKER_REGISTRY=your-registry.com
./build-base.sh

# With version tag
export VERSION=1.2.3
./build-base.sh
```

**Features:**
- Colored output
- Build time tracking
- Automatic registry push
- Version tagging

---

### build-app.sh

Builds the application using the optimized Dockerfile with BuildKit caching.

**Usage:**
```bash
./build-app.sh

# With registry
export DOCKER_REGISTRY=your-registry.com
./build-app.sh

# With version tag
export VERSION=1.2.3
./build-app.sh
```

**Features:**
- BuildKit cache mounts
- Layer optimization
- Automatic base image check
- Registry push support

---

### build-app-fast.sh

Ultra-fast build for development. Builds JAR locally, then creates minimal Docker image.

**Usage:**
```bash
./build-app-fast.sh
```

**Features:**
- Fastest build time (~30-60 seconds)
- Local Gradle build
- Minimal Docker layers
- Performance metrics

---

### benchmark-build.sh

Comprehensive performance benchmark comparing all build strategies.

**Usage:**
```bash
./benchmark-build.sh
```

**Tests:**
1. Full build (no cache)
2. Cached build (no changes)
3. JAR-only change
4. Fast build

**Output:**
```
Build Type                     Time            vs Full Build
----------                     ----            -------------
Full Build (no cache)          8m 23s          baseline
Cached Build (no changes)      45s             -91%
JAR Change (optimized)         1m 32s          -82%
Fast Build (local JAR)         52s             -90%
```

---

## File Structure

```
docker/
├── Dockerfile.base           # Base image with system dependencies
├── Dockerfile.app            # Optimized application build
├── Dockerfile.app-fast       # Ultra-fast development build
├── Dockerfile                # Legacy Dockerfile (kept for compatibility)
├── build-base.sh             # Build base image script
├── build-app.sh              # Build application script
├── build-app-fast.sh         # Fast build script
├── benchmark-build.sh        # Performance benchmarking
├── docker-compose.yml        # Docker Compose configuration
├── docker-entrypoint.sh      # Container entrypoint script
├── agent.yml                 # Agent configuration
├── Makefile                  # Convenient build commands
├── BUILD_OPTIMIZATION.md     # Detailed optimization guide
└── README.md                 # This file
```

---

## Environment Variables

### Build Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `DOCKER_REGISTRY` | Docker registry URL | (none) |
| `VERSION` | Image version tag | `latest` |
| `BASE_IMAGE_TAG` | Base image tag | `latest` |
| `APP_IMAGE_TAG` | Application image tag | `latest` |

### Runtime Configuration

See `docker-compose.yml` for full list of runtime environment variables.

---

## Troubleshooting

### Base Image Not Found

**Error:**
```
ERROR: Base image not found: qodo/command-sdk-base:latest
```

**Solution:**
```bash
make base
```

---

### Gradle Build Fails

**Error:**
```
ERROR: Gradle build failed!
```

**Solution:**
```bash
# Clean Gradle cache
cd ..
./gradlew clean

# Try again
cd docker
make fast
```

---

### Docker Cache Issues

**Error:**
```
Build is slow despite using cached layers
```

**Solution:**
```bash
# Clear Docker build cache
make clean-all

# Rebuild from scratch
make base
make fast
```

---

### BuildKit Not Enabled

**Error:**
```
BuildKit features not working
```

**Solution:**
```bash
# Enable BuildKit
export DOCKER_BUILDKIT=1

# Or add to ~/.bashrc or ~/.zshrc
echo 'export DOCKER_BUILDKIT=1' >> ~/.bashrc
```

---

## Best Practices

### 1. Use Fast Build for Development

```bash
# Add alias to your shell
alias docker-fast='cd docker && make fast && cd ..'
```

### 2. Enable BuildKit Globally

Add to `~/.docker/config.json`:
```json
{
  "features": {
    "buildkit": true
  }
}
```

### 3. Monitor Build Times

```bash
# Time your builds
time make fast
```

### 4. Regular Maintenance

```bash
# Monthly: Rebuild base image
make base

# Weekly: Clean unused images
docker image prune -a
```

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Build Docker Image

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2
      
      - name: Build base image
        run: cd docker && make base
        
      - name: Build application
        run: cd docker && make app
        
      - name: Push to registry
        env:
          DOCKER_REGISTRY: ${{ secrets.DOCKER_REGISTRY }}
        run: cd docker && make push
```

### GitLab CI

```yaml
build:
  stage: build
  script:
    - cd docker
    - make base
    - make app
  only:
    - main
```

---

## Advanced Usage

### Custom Registry

```bash
# Set registry
export DOCKER_REGISTRY=your-registry.com

# Build and push
make base
make app
make push
```

### Version Tagging

```bash
# Build with version
export VERSION=1.2.3
make app

# Creates tags:
# - qodo/command-sdk:latest
# - qodo/command-sdk:1.2.3
```

### Multi-Architecture Builds

```bash
# Enable buildx
docker buildx create --use

# Build for multiple platforms
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f Dockerfile.base \
  -t qodo/command-sdk-base:latest \
  --push \
  .
```

---

## Performance Tips

### 1. Use Fast Build for Development

The fast build is **85-90% faster** than full builds:

```bash
make fast  # 30-60 seconds
```

### 2. Leverage BuildKit Cache

BuildKit cache mounts persist Gradle dependencies:

```bash
export DOCKER_BUILDKIT=1
make app
```

### 3. Minimize Layer Changes

Keep frequently changing files (source code) in later layers.

### 4. Use .dockerignore

Exclude unnecessary files from build context:

```
.git
.idea
*.md
node_modules
build
.gradle
```

---

## Monitoring

### Image Sizes

```bash
make stats
```

### Build Performance

```bash
make benchmark
```

### Application Health

```bash
make health
```

---

## Support

For issues or questions:
- See [BUILD_OPTIMIZATION.md](BUILD_OPTIMIZATION.md) for detailed guide
- Check [troubleshooting section](#troubleshooting)
- Review [main README](../README.md) for project documentation

---

## Summary

✅ **85-90% faster builds** for code changes  
✅ **Efficient layer caching** for dependencies  
✅ **Flexible build strategies** for different scenarios  
✅ **BuildKit optimizations** for maximum performance  
✅ **Easy CI/CD integration**  
✅ **Convenient Makefile commands**  

**Recommended workflow:**
```bash
make base      # Once per month
make fast      # Daily development
make run       # Start application
```
