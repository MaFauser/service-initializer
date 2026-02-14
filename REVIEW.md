# Project review summary

Review focused on **simplicity**, **quality**, and **using built-in libraries** instead of custom code. File-by-file and area-by-area.

---

## Changes made

### 1. Build: remove unused REST Docs and Asciidoctor
- **Removed:** `org.asciidoctor.jvm.convert` plugin, `spring-boot-restdocs`, `spring-restdocs-mockmvc`, `snippetsDir`, asciidoctor task, and test `outputs.dir(snippetsDir)`.
- **Reason:** No test uses `@AutoConfigureRestDocs` or `.andDo(document(...))`, so no snippets are generated. REST Docs and Asciidoctor were unused and added complexity.

### 2. Tests: consistent Mockito-Kotlin usage (ExampleServiceTest)
- **Changed:** Replaced `org.mockito.ArgumentMatchers.any()` / `anyString()` with `org.mockito.kotlin.any<T>()` (e.g. `any<Example>()`, `any<String>()`, `any<UUID>()`).
- **Reason:** Single style and fewer imports; mockito-kotlin is already a dependency.

### 3. Docs: REST controller and REST integration test
- **DOMAINS.md:** Documented `ExampleController` (REST), `ExampleRestControllerIntegrationTest`, and added REST controller to the “new domain” steps.
- **README.md:** Domain table now has separate rows for REST (`ExampleController.kt`) and GraphQL (`ExampleGraphQLController.kt`).
- **Reason:** REST API and its tests were missing from the written structure.

### 4. ObjectMapper in REST integration test (reverted)
- **Tried:** Inject `ObjectMapper` from the test context instead of `jacksonObjectMapper()`.
- **Reverted:** With `@SpringBootTest(webEnvironment = MOCK)`, no qualifying `ObjectMapper` bean was available (likely due to Spring Boot 4 / Jackson setup). Kept `jacksonObjectMapper()` so tests stay reliable.

### 5. Package naming cleanup (no package-derived names)
- **Rule:** Use domain/feature-based names, not names derived from package path (see `.cursor/rules/no-package-naming.mdc`).
- **Helm chart:** Renamed from **service-chart** (path-derived: `helm/service-chart`) to **stack** (semantic: deploys the full stack). Folder `helm/service-chart` → `helm/stack`, Chart name and all template helpers `service-chart.*` → `stack.*`.
- **References updated:** `application-k8s.yaml` (myservice-stack-*), scripts (`k8s-local.sh`, `deploy.sh`), workflows (pr-validation, deploy-dev, release), `.vscode/settings.json`, DEPLOYMENT.md, helm/README.md, .github/SETUP.md. K8s resource names are now e.g. `dev-stack-app`, `myservice-stack-postgresql`.

---

## Reviewed and left as-is (no change)

### Application and domain code
- **Application.kt** – Top-level `main`, no companion/`@JvmStatic`; minimal and idiomatic.
- **BaseEntity** – `@CreationTimestamp` / `@UpdateTimestamp`; no custom equals/hashCode; clear and minimal.
- **Example** – Extends `BaseEntity`, constructor `id` passed to super; no redundant override.
- **ExampleRepository** – Plain Spring Data JPA; `findByName`, `existsByName` are enough.
- **ExampleService** – Uses `Optional.orElse(null)`; acceptable. No need for extra Kotlin extensions.
- **ExampleController** – REST CRUD; delegates to service; appropriate.
- **ExampleExceptionHandler** – Maps domain exceptions to 404/409; minimal.
- **ExampleGraphQLController** – `parseUuid` / `parseUuidOrNull` are small helpers; keeping them is fine (no heavy “reimplementing the wheel”).

### Configuration
- **application.yaml / application-k8s.yaml** – Structure and keys (e.g. `'[format_sql]'`) are correct. No duplication to remove.
- **logback-spring.xml** – Uses Spring Boot defaults and console; no extra appenders.
- **V1__create_example_table.sql** – Table and unique index on `name`; matches entity.

### Tests
- **ApplicationTests** – Context load with Testcontainers; good.
- **TestApplication** – Optional entry point with Testcontainers; useful for IDE/local runs.
- **TestcontainersConfiguration** – PostgreSQL, Redis, Kafka, LGTM stack; appropriate for stack coverage.
- **ExampleTest** – Covers reference equality, `toString`, setters; sufficient for the entity.
- **ExampleControllerIntegrationTest** – GraphQL integration with `GraphQlTester`; good.
- **ExampleRestControllerIntegrationTest** – REST integration with MockMvc; good. Keeps `jacksonObjectMapper()` after revert.

### Build and tooling
- **build.gradle.kts** – Kotlin, Spring Boot, ktlint, JaCoCo, debug for `bootRun`, `printClasspath` for IDE; dependencies aligned with features (no unused libs beyond the removed REST Docs).
- **settings.gradle.kts** – Minimal.
- **.gitignore** – Covers `bin/`, `build/`, IDE, local config; good.

### Docs and scripts
- **README.md** – Run instructions, links, domain table (updated with REST).
- **DEPLOYMENT.md** – Environments and flows; useful.
- **DOMAINS.md** – Domain layout and “new domain” steps (updated with REST).
- **DOCKER.md** – Docker Compose usage; useful.
- **HELP.md** – Spring Boot reference links; keep as-is.
- **Scripts** – `k8s-local.sh`, `docker-local.sh`, `deploy.sh`, `release.sh` – Used by docs/CI; no change.

### CI/CD
- **pr-validation.yml** – Build, ktlint, tests, JaCoCo artifact, Docker image, Helm lint; coherent.
- **deploy-dev.yml**, **release.yml** – Not modified; consistent with deployment docs.

### Other
- **Helm chart** – Structure and templates; no unnecessary files.
- **.cursor/rules** – Coverage, naming, versions; kept.
- **qodana.yaml** – Uses `projectJDK: "25"` while app is on 21; left as-is (CI/tooling choice).

---

## Optional follow-ups (not done)

- **Qodana JDK:** Align `qodana.yaml` `projectJDK` with the project (e.g. 21) if you want analysis on the same JDK.
- **HELP.md:** Remove or shorten if you prefer not to maintain Spring Boot reference links; current content is harmless.

---

## Summary

- **Removed:** Unused REST Docs and Asciidoctor (plugin, deps, tasks, snippetsDir).
- **Simplified:** ExampleServiceTest uses mockito-kotlin `any<T>()` only.
- **Documented:** REST controller and REST integration test in DOMAINS.md and README.
- **Reverted:** ObjectMapper injection in REST integration test; kept `jacksonObjectMapper()` for compatibility with MOCK web environment.

Everything else was reviewed and considered already simple and of good quality, with no extra custom code where built-in or existing libraries suffice.
