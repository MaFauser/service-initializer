# Service Initializer – common commands
# Usage: make [target]

.PHONY: help config build run run-debug test check clean lint format coverage coverage-check \
	docker-up docker-down docker-logs docker-status docker-restart docker-reset deps boot-jar

# Default: show help
help:
	@echo "Service Initializer – make targets"
	@echo ""
	@echo "  config          Generate .env from helm/stack/config/shared.yaml (run before first docker-up)"
	@echo "  build           Compile, ktlint, produce JAR (no tests)"
	@echo "  run             Start the app (./gradlew bootRun)"
	@echo "  run-debug       Start the app with remote debug (port 5005, suspend=y)"
	@echo "  test            Run tests (Testcontainers; Docker required)"
	@echo "  check           Run ktlint + tests (same as CI check)"
	@echo "  clean           Clean build outputs"
	@echo "  lint            Run ktlint check"
	@echo "  lint-fix        Run ktlint format"
	@echo "  coverage        Run tests and open JaCoCo HTML report"
	@echo "  coverage-check  Run tests and verify 90% line coverage"
	@echo "  deps            Refresh dependencies (--refresh-dependencies)"
	@echo "  boot-jar        Build executable JAR (no tests)"
	@echo ""
	@echo "Docker Compose (local stack: Postgres, Redis, Kafka, Grafana, etc.)"
	@echo "  docker-up       Start all services (docker compose up -d)"
	@echo "  docker-down     Stop all services"
	@echo "  docker-logs     Follow logs"
	@echo "  docker-status   Show service status"
	@echo "  docker-restart  Restart all services"
	@echo "  docker-reset    Stop and remove volumes (interactive; deletes data)"
	@echo ""

# Generate .env from shared.yaml (requires yq). Run before first docker-up.
config:
	./scripts/load-config.sh

# Build (compile, ktlint, JAR; no tests)
build:
	./gradlew build -x test

# Run the application
run:
	./gradlew bootRun

# Run with remote debug (default port 5005). Use: make run-debug DEBUG_PORT=5006
run-debug:
	./gradlew bootRun -Pdebug=true $(if $(DEBUG_PORT),-PdebugPort=$(DEBUG_PORT),)

# Run tests (Testcontainers; requires Docker)
test:
	./gradlew test

# Lint + test (CI check)
check:
	./gradlew check

# Clean
clean:
	./gradlew clean

# Kotlin lint
lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

# Test + JaCoCo report; open HTML report on macOS
coverage:
	./gradlew test jacocoTestReport
	@open build/reports/jacoco/test/html/index.html 2>/dev/null || true

# Test + coverage verification (90% line minimum)
coverage-check:
	./gradlew test jacocoTestCoverageVerification

# Refresh dependencies
deps:
	./gradlew build --refresh-dependencies

# Build boot JAR only (skip tests)
boot-jar:
	./gradlew bootJar -x test

# --- Docker Compose (local dev stack) ---
# Ensure .env exists first: make config (requires yq)

docker-up:
	./scripts/docker-local.sh start

docker-down:
	./scripts/docker-local.sh stop

docker-logs:
	./scripts/docker-local.sh logs

docker-status:
	./scripts/docker-local.sh status

docker-restart:
	./scripts/docker-local.sh restart

# Interactive: prompts for confirmation before deleting volumes
docker-reset:
	./scripts/docker-local.sh reset
