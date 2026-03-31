.PHONY: build run run-debug test check clean lint format coverage deps bootjar api

-include .env
export

ifeq ($(OS),Windows_NT)
GRADLEW := gradlew.bat
else
GRADLEW := ./gradlew
endif

build:
	$(GRADLEW) build -x test

run:
	$(GRADLEW) bootRun

run-debug:
	$(GRADLEW) bootRun -Pdebug=true $(if $(DEBUG_PORT),-PdebugPort=$(DEBUG_PORT),)

test:
	$(GRADLEW) test

check:
	$(GRADLEW) check

clean:
	$(GRADLEW) clean

lint:
	$(GRADLEW) ktlintCheck

format:
	$(GRADLEW) ktlintFormat

coverage:
	$(GRADLEW) test jacocoTestReport
	@open build/reports/jacoco/test/html/index.html 2>/dev/null || true

deps:
	$(GRADLEW) build --refresh-dependencies

bootjar:
	$(GRADLEW) bootJar -x test

api:
	./scripts/api-test.sh
