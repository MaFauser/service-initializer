# Service Initializer

Spring Boot + PostgreSQL, Redis, Kafka, GraphQL. Template for adding your own domains.

## Run locally

```bash
make config          # once: generate .env from helm/stack/config/shared.yaml
make docker-up       # Postgres, Redis, Kafka, Grafana, Tempo, Prometheus
make run             # app on :8081
```

**Tests:** `make test` (Testcontainers, needs Docker)

## Local URLs

| Service | URL |
|---------|-----|
| GraphiQL | http://localhost:8081/graphiql |
| Health | http://localhost:8081/actuator/health |
| pgAdmin | http://localhost:5050 (admin@local.dev / admin) |
| Kafka UI | http://localhost:8080 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

## Deploy

- **Local K8s (Docker Desktop):** `./scripts/k8s-local.sh up` — see [DEPLOYMENT.md](DEPLOYMENT.md)
- **Dev (remote cluster):** Create Secrets per [docs/SECRETS.md](docs/SECRETS.md), then `./scripts/k8s-dev.sh deploy`. Port-forward: `./scripts/k8s-dev.sh forward`
- **Prod:** [docs/SECRETS.md](docs/SECRETS.md) + [DEPLOYMENT.md](DEPLOYMENT.md)

## Project layout

```
src/main/kotlin/.../service/
├── config/                  # Exception handlers, tracing
├── example/                 # Example domain (entity, repo, service, REST, GraphQL)
src/main/resources/
├── application.yaml
├── application-k8s.yaml
├── graphql/example/schema.graphqls
└── db/migration/            # Flyway
helm/stack/                  # K8s: app, Postgres, Redis, Kafka, OpenSearch, Grafana, etc.
├── config/shared.yaml       # Single source for images/credentials
└── values*.yaml
```

**Add a domain:** Copy `example` package and `graphql/example`, add Flyway migration. See [DOMAINS.md](DOMAINS.md).

## Docs

- [SECRETS.md](docs/SECRETS.md) — Kubernetes and GitHub secrets for dev/prod
- [DEPLOYMENT.md](DEPLOYMENT.md) — Helm install (local, dev, prod)
- [OBSERVABILITY.md](docs/OBSERVABILITY.md) — Logs, metrics, traces (OpenSearch, Prometheus, Tempo)
