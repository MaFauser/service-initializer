# Service Initializer

Default backend service: Spring Boot, PostgreSQL, Redis, Kafka, GraphQL. Use this repo as a template to add your own domains.

## Project Structure

```
service-initializer/
├── src/main/kotlin/.../service/
│   ├── Application.kt                 # Entry point
│   ├── BaseEntity.kt                  # Shared: id, createdAt, updatedAt for all entities
│   ├── config/                        # App-wide: exceptions, tracing, logging
│   │   ├── ConflictException.kt       # Duplicate constraint → 409
│   │   ├── GlobalExceptionHandler.kt  # Maps shared exceptions → HTTP status
│   │   ├── InvalidIdException.kt      # Invalid UUID (GraphQL) → 400
│   │   ├── NotFoundException.kt       # Resource missing → 404
│   │   ├── NoiseExclusions.kt
│   │   └── TracingConfiguration.kt
│   └── example/                      # Example domain (copy to add new domains)
│       ├── Example.kt                # Entity
│       ├── ExampleRepository.kt
│       ├── ExampleService.kt         # + input DTOs, domain exceptions
│       ├── ExampleController.kt       # REST
│       └── ExampleGraphQLController.kt
├── src/main/resources/
│   ├── application.yaml              # Local config (env vars with defaults)
│   ├── application-k8s.yaml          # K8s profile
│   ├── graphql/example/schema.graphqls
│   └── db/migration/
├── helm/stack/                        # K8s stack (Postgres, Redis, Kafka, Grafana, etc.)
│   ├── config/shared.yaml            # Single source: images, credentials (Docker + Helm)
│   └── values*.yaml
├── docker-compose.yml                # Local dev (requires ./scripts/load-config.sh first)
├── grafana/provisioning/             # Grafana datasources + dashboards
└── scripts/load-config.sh            # Generates .env from shared.yaml
```

## Domain structure (package-by-feature)

Code is organized **by domain** so each feature lives in one place and is easy to copy when adding new ones.

| Layer        | Location (example domain)        | Role |
|--------------|-----------------------------------|------|
| Entity       | `example/Example.kt`              | JPA entity, table mapping |
| Repository   | `example/ExampleRepository.kt`    | Spring Data JPA, persistence |
| Service      | `example/ExampleService.kt`       | Business logic, transactions |
| REST         | `example/ExampleController.kt`    | REST CRUD at `/examples` |
| GraphQL      | `example/ExampleGraphQLController.kt` | GraphQL queries/mutations |
| Schema       | `graphql/example/schema.graphqls` | GraphQL types and operations |
| Migration    | `db/migration/V*__*.sql`          | Flyway, table DDL |

**Adding a new domain:** duplicate the `example` package and `graphql/example` schema, rename to your domain, then add a Flyway migration for the new table. See [DOMAINS.md](DOMAINS.md) for a step-by-step.

## Run

1. **First time:** Run `./scripts/load-config.sh` to generate `.env` and Prometheus config from `helm/stack/config/shared.yaml` (single source for Docker + Helm).
2. Start dependencies: `docker compose up -d`
3. Run the app: **Gradle** → `bootRun` task

**Tests:** `./gradlew test` (uses Testcontainers; requires Docker). For IDE run with Testcontainers, use `TestApplication` main.

## Links (when running locally)

| Service | URL |
|---------|-----|
| pgAdmin (PostgreSQL UI) | http://localhost:5050 (admin@local.dev / admin) |
| GraphiQL | http://localhost:8081/graphiql |
| Health | http://localhost:8081/actuator/health |
| Info | http://localhost:8081/actuator/info |
| Prometheus (actuator) | http://localhost:8081/actuator/prometheus |
| Kafka UI | http://localhost:8080 |
| Grafana | http://localhost:3000 (pre-provisioned: Prometheus, Tempo, Spring Boot dashboard in "Service" folder) |
| Prometheus | http://localhost:9090 |

## Deploying to Kubernetes

- **Local K8s (Docker Desktop, kind):** Use `values-local.yaml` – no Secrets needed; credentials are in the file. See [DEPLOYMENT.md](DEPLOYMENT.md).
- **Dev or prod:** Use Secrets only (no passwords in values). Create the required Kubernetes Secrets **before** `helm install`. See **[Creating Secrets (dev/prod)](docs/SECRETS.md)** for exact Secret names, keys, and step-by-step commands. For CI/CD, configure the GitHub repository secrets listed in that doc.

## Reference Documentation

* [Creating Secrets for dev/prod](docs/SECRETS.md) – Kubernetes and GitHub Actions secrets to run the app with values-dev or values-prod
* [Deployment guide](DEPLOYMENT.md) – Local, dev, and prod Helm install
* [Observability: Logs, Metrics, Traces](docs/OBSERVABILITY.md) – where to view logs (`kubectl logs`), Prometheus (metrics), Tempo (traces)
* [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/gradle-plugin)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/actuator/index.html)
