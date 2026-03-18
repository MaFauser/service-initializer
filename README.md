# Service Initializer

Spring Boot + PostgreSQL, Redis, Kafka, GraphQL. Template for building microservices.

## Getting started

After creating a repo from this template, run the interactive setup to configure your project:

```bash
./setup.sh
```

This will prompt for your service name, package, database name, and update everything across the codebase. The script removes itself after running.

## Run locally

```bash
docker compose up -d    # start Postgres, Redis, Kafka
make run                # app on :8081
make test               # Testcontainers, needs Docker
```

## Services (local dev)

| Service    | URL                          |
|------------|------------------------------|
| GraphiQL   | http://localhost:8081/graphiql |
| Health     | http://localhost:8081/actuator/health |
| pgAdmin    | http://localhost:5050         |
| Kafka UI   | http://localhost:8080         |

## Project layout

```
src/main/kotlin/.../service/
├── config/                      # Web, tracing, rate limiting, GraphQL
├── example/                     # Example domain (entity, repo, service, REST, GraphQL)
├── exception/                   # Global exception handlers
src/main/resources/
├── application.yaml
├── application-k8s.yaml
├── graphql/example/schema.graphqls
└── db/migration/                # Flyway
```

**Add a domain:** Copy `example` package and `graphql/example`, add Flyway migration. See [DOMAINS.md](docs/DOMAINS.md).

## CI/CD

GitHub Actions workflows build, test, and push container images to GHCR:

- **PR validation** — build + test + coverage + image push
- **Dev push** — build + push image tagged `dev-latest`
- **Release** — retag image with version + `latest`

To release, create a GitHub Release from the [Releases page](../../releases) (or `gh release create v1.0.0 --generate-notes`).

Deployment to Kubernetes is handled by the [platform-infra](https://github.com/MaFauser/platform-infra) repo.

## Docs

- [DOMAINS.md](docs/DOMAINS.md) — Domain structure and adding new domains
- [DOCKER.md](docs/DOCKER.md) — Docker Compose local development
