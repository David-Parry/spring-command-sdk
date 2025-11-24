# Makefile for Docker Build Optimization
# Provides convenient shortcuts for building Docker images

.PHONY: help base app fast benchmark clean test run stop logs shell

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[1;33m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(BLUE)Docker Build Optimization - Available Commands$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-15s$(NC) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(YELLOW)Quick Start:$(NC)"
	@echo "  1. make base     # Build base image (one-time, ~5-10 min)"
	@echo "  2. make fast     # Fast build for development (~30-60 sec)"
	@echo "  3. make run      # Start the application"
	@echo ""

base: ## Build base Docker image (run rarely, ~5-10 min)
	@echo "$(BLUE)Building base image...$(NC)"
	@./build-base.sh

app: ## Build application image (standard build, ~1-2 min)
	@echo "$(BLUE)Building application image...$(NC)"
	@./build-app.sh

fast: ## Fast build for development (recommended, ~30-60 sec)
	@echo "$(BLUE)Fast building application...$(NC)"
	@./build-app-fast.sh

benchmark: ## Run build performance benchmark
	@echo "$(BLUE)Running build benchmark...$(NC)"
	@./benchmark-build.sh

clean: ## Clean Docker images and build cache
	@echo "$(YELLOW)Cleaning Docker images and cache...$(NC)"
	@docker rmi qodo/command-sdk:latest qodo/command-sdk:fast-dev 2>/dev/null || true
	@docker builder prune -f
	@echo "$(GREEN)✓ Cleanup complete$(NC)"

clean-all: ## Clean everything including base image
	@echo "$(YELLOW)Cleaning all Docker images and cache...$(NC)"
	@docker rmi qodo/command-sdk:latest qodo/command-sdk:fast-dev qodo/command-sdk-base:latest 2>/dev/null || true
	@docker builder prune -af
	@echo "$(GREEN)✓ Full cleanup complete$(NC)"

test: ## Test the Docker image
	@echo "$(BLUE)Testing Docker image...$(NC)"
	@docker run --rm qodo/command-sdk:latest java --version
	@docker run --rm qodo/command-sdk:latest node --version
	@docker run --rm qodo/command-sdk:latest snyk --version
	@echo "$(GREEN)✓ All tests passed$(NC)"

run: ## Run the application with docker-compose
	@echo "$(BLUE)Starting application...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose up -d; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose up -d; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Application started$(NC)"
	@echo "$(YELLOW)Access at: http://localhost:8081$(NC)"

stop: ## Stop the application
	@echo "$(YELLOW)Stopping application...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose down; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose down; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Application stopped$(NC)"

restart: fast ## Fast rebuild and restart (development workflow)
	@echo "$(BLUE)Restarting application...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose restart command-sdk; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose restart command-sdk; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Application restarted$(NC)"

logs: ## Show application logs
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose logs -f command-sdk; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose logs -f command-sdk; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi

shell: ## Open shell in running container
	@docker exec -it command-sdk-app /bin/bash

stats: ## Show Docker image sizes
	@echo "$(BLUE)Docker Image Statistics:$(NC)"
	@echo ""
	@docker images qodo/command-sdk-base --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
	@docker images qodo/command-sdk --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
	@echo ""

info: ## Show build system information
	@echo "$(BLUE)Build System Information:$(NC)"
	@echo ""
	@echo "$(GREEN)Base Image:$(NC)"
	@docker image inspect qodo/command-sdk-base:latest --format '  Size: {{.Size}} bytes ({{.VirtualSize}} virtual)' 2>/dev/null || echo "  Not built yet"
	@echo ""
	@echo "$(GREEN)Application Image:$(NC)"
	@docker image inspect qodo/command-sdk:latest --format '  Size: {{.Size}} bytes ({{.VirtualSize}} virtual)' 2>/dev/null || echo "  Not built yet"
	@echo ""
	@echo "$(GREEN)BuildKit Status:$(NC)"
	@docker buildx version 2>/dev/null || echo "  BuildKit not available"
	@echo ""

# Development workflow shortcuts
dev-setup: base fast ## Complete development setup (base + fast build)
	@echo "$(GREEN)✓ Development environment ready!$(NC)"

dev-rebuild: fast restart ## Quick development cycle (rebuild + restart)
	@echo "$(GREEN)✓ Development rebuild complete!$(NC)"

# CI/CD workflow
ci-build: base app ## CI/CD build workflow
	@echo "$(GREEN)✓ CI/CD build complete!$(NC)"

# Push to registry
push: ## Push images to registry (requires DOCKER_REGISTRY env var)
	@if [ -z "$$DOCKER_REGISTRY" ]; then \
		echo "$(YELLOW)Warning: DOCKER_REGISTRY not set$(NC)"; \
		echo "Set it with: export DOCKER_REGISTRY=your-registry.com"; \
		exit 1; \
	fi
	@echo "$(BLUE)Pushing to registry: $$DOCKER_REGISTRY$(NC)"
	@docker tag qodo/command-sdk-base:latest $$DOCKER_REGISTRY/qodo/command-sdk-base:latest
	@docker tag qodo/command-sdk:latest $$DOCKER_REGISTRY/qodo/command-sdk:latest
	@docker push $$DOCKER_REGISTRY/qodo/command-sdk-base:latest
	@docker push $$DOCKER_REGISTRY/qodo/command-sdk:latest
	@echo "$(GREEN)✓ Images pushed successfully$(NC)"

# Health check
health: ## Check application health
	@echo "$(BLUE)Checking application health...$(NC)"
	@curl -s http://localhost:8081/actuator/health | jq . || echo "$(YELLOW)Application not running or jq not installed$(NC)"

# Version info
version: ## Show version information
	@echo "$(BLUE)Version Information:$(NC)"
	@echo ""
	@echo "$(GREEN)Java:$(NC)"
	@docker run --rm qodo/command-sdk:latest java --version 2>/dev/null || echo "  Image not built"
	@echo ""
	@echo "$(GREEN)Node.js:$(NC)"
	@docker run --rm qodo/command-sdk:latest node --version 2>/dev/null || echo "  Image not built"
	@echo ""
	@echo "$(GREEN)Snyk:$(NC)"
	@docker run --rm qodo/command-sdk:latest snyk --version 2>/dev/null || echo "  Image not built"
	@echo ""
