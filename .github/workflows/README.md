# GitHub Actions Workflows

## Workflows

### 1. PR Validation (`pr-validation.yml`)
- **Trigger**: Pull requests to `main`, `dev`, or `testing` branches
- **Concurrency**: Cancels in-progress runs when a new push is made to the same PR
- **Jobs**:
  - **Build & Test**
    - Runs `./gradlew build` (ktlintCheck, test, jacocoTestReport, assemble)
    - Uploads JaCoCo HTML report as artifact (`jacoco-report`, 7 days)
    - Builds Docker image for validation
    - Comments on PR with success or failure
  - **Lint (ktlint + Helm)**
    - Runs `./gradlew ktlintCheck` (fails the job if Kotlin style violations)
    - Lints Helm chart and value files (dev + prod)
    - Validates rendered Kubernetes manifests
- **Artifacts**: Download "jacoco-report" from the workflow run to view coverage locally.

### 2. Deploy to Dev (`deploy-dev.yml`)
- **Trigger**: Push to `dev` branch
- **Steps**: Run tests → Build Docker image → Tag `dev-{sha}` and `dev-latest` → Push to GHCR → Deploy to dev cluster with Helm
- **Image tags**: `dev-{commit-sha}` and `dev-latest`

### 3. Release to Production (`release.yml`)
- **Trigger**: GitHub Release published
- **Steps**: Pull dev image → Retag as version + latest → Push → Deploy to prod cluster with Helm
- **Image tags**: `{version}` (e.g. `v1.0.0`) and `latest`

### 4. Qodana (`qodana_code_quality.yml`)
- **Trigger**: Manual, pull_request, or push to `main`
- **Purpose**: JetBrains Qodana code analysis (optional; requires `QODANA_TOKEN` for full features)

## Quick Start

See [SETUP.md](SETUP.md) for configuration.

### TL;DR

1. **Secrets** (Settings → Secrets and variables → Actions):
   - `KUBECONFIG_DEV` – Dev cluster kubeconfig
   - `KUBECONFIG_PROD` – Prod cluster kubeconfig
   - `DB_PASSWORD` – Production DB password

2. **Deploy to dev**: `git push origin dev`

3. **Deploy to prod**: Create a release (e.g. `./scripts/release.sh v1.0.0`)

## Coverage

- JaCoCo runs as part of `./gradlew build` (test is finalized by jacocoTestReport).
- PR Validation uploads the HTML report; download the `jacoco-report` artifact to view it.
- Entity classes are excluded from coverage (see `.cursor/rules/coverage.mdc` and `build.gradle.kts`).
