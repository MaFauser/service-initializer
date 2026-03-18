# GitHub Actions Workflows

## Workflows

### 1. PR Validation (`pr-validation.yml`)
- **Trigger**: Pull requests to `main`, `dev`, or `testing` branches
- **Concurrency**: Cancels in-progress runs when a new push is made to the same PR
- **Jobs**:
  - **Build & Test**: Runs `./gradlew build` with coverage verification, uploads JaCoCo report
  - **Push Image**: Builds and pushes Docker image to GHCR (skipped if unchanged)

### 2. Push Dev Image (`deploy-dev.yml`)
- **Trigger**: Push to `dev` branch
- **Steps**: Build Docker image, push to GHCR, tag as `dev-latest`

### 3. Release Image (`release.yml`)
- **Trigger**: GitHub Release published
- **Steps**: Retag existing image with version + `latest`

Deployment to Kubernetes is handled by the [platform-infra](../../../platform-infra) repo.

## Quick Start

See [SETUP.md](../SETUP.md) for configuration.

### TL;DR

1. **Deploy to dev**: `git push origin dev` (builds + pushes image)
2. **Release**: `./scripts/release.sh v1.0.0` (retags image)

## Coverage

- JaCoCo runs as part of `./gradlew build` (test is finalized by jacocoTestReport).
- PR Validation uploads the HTML report; download the `jacoco-report` artifact to view it.
- Entity classes are excluded from coverage (see `.cursor/rules/coverage.mdc` and `build.gradle.kts`).
