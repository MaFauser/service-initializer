# Service Initializer – common commands
# Usage: make [target]

.PHONY: help config build run run-debug test check clean lint format coverage coverage-check deps bootjar

# Default: show help
help:
	@echo "Service Initializer – make targets"
	@echo ""
	@echo "  config          Generate .env from helm/stack/config/shared.yaml"
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
	@echo "  bootjar         Build executable JAR (no tests)"
	@echo ""

config:
	./scripts/load-config.sh

build:
	./gradlew build -x test

run:
	./gradlew bootRun

run-debug:
	./gradlew bootRun -Pdebug=true $(if $(DEBUG_PORT),-PdebugPort=$(DEBUG_PORT),)

test:
	./gradlew test

check:
	./gradlew check

clean:
	./gradlew clean

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

coverage:
	./gradlew test jacocoTestReport
	@open build/reports/jacoco/test/html/index.html 2>/dev/null || true

deps:
	./gradlew build --refresh-dependencies

bootjar:
	./gradlew bootJar -x test
