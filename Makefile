.PHONY: config build run run-debug test check clean lint format coverage deps bootjar api

-include .env
export

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

api:
	./scripts/api-test.sh
