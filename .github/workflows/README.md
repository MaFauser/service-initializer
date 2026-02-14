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
  - **Coverage (≥90%)**
    - Runs `./gradlew jacocoTestCoverageVerification` (fails the job if line coverage &lt; 90%)
  - **Sonar**
    - Runs `./gradlew test jacocoTestReport sonar` (SonarCloud analysis). **Skipped** if `SONAR_TOKEN` is not set.
    - Requires: repo secret `SONAR_TOKEN`; optional repo variables `SONAR_ORGANIZATION`, `SONAR_PROJECT_KEY` (default: `github.repository`).
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
- The **Coverage (≥90%)** job fails the PR if line coverage is below 90%.

## SonarCloud (optional)

1. Create a project at [sonarcloud.io](https://sonarcloud.io) and get your organization key and project key.
2. Generate a token in SonarCloud (My Account → Security → Generate Tokens).
3. In the repo: **Settings → Secrets and variables → Actions**:
   - Add secret **`SONAR_TOKEN`** (the SonarCloud token).
   - Optionally add variables **`SONAR_ORGANIZATION`** and **`SONAR_PROJECT_KEY`** (default project key is `owner/repo`).
4. The **Sonar** job runs only when `SONAR_TOKEN` is set; it runs `./gradlew test jacocoTestReport sonar` and reports to SonarCloud.

## Qodana (optional)

Qodana runs **JetBrains-style code analysis** (IntelliJ inspections) on JVM code (Java/Kotlin). It complements ktlint (style) and Sonar (coverage, security, duplication) with IDE-level checks in CI.

**What it runs:** Profile `qodana.starter` + linter `jetbrains/qodana-jvm-community` (see `qodana.yaml`). Results appear as PR comments and annotations.

**Types of checks (examples):**

- **Potential bugs:** Possible NPE, unreachable code, suspicious type comparisons.
- **Code quality:** Method/class complexity, duplicate code, long parameter lists.
- **Dead code:** Unused variables, parameters, private methods, unused imports.
- **Style / conventions:** Naming (e.g. constants), redundant modifiers.
- **Kotlin:** Prefer `require`/`check`, safe calls, scope functions; redundant `else`, unnecessary `run`/`let`.

**Setup:** Add secret **`QODANA_TOKEN`** in repo Settings → Secrets and variables → Actions. The Qodana workflow runs on manual, PR, and push to `main`; full features (e.g. PR comments) require the token.
