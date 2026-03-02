# Service Initializer

Spring Boot + PostgreSQL, Redis, Kafka, GraphQL. Template for adding your own domains.

## Run locally

```bash
make config          # load .env based on default configs from infra/helm locally
make run             # app on :8081 (needs local Postgres/Redis/Kafka or use K8s)
make test            # Testcontainers, needs Docker
```

## Deploy

- **Local K8s (Docker Desktop):** `./scripts/k8s-local.sh up` — see [DEPLOYMENT.md](docs/DEPLOYMENT.md)
- **Dev (remote cluster):** `./scripts/k8s-dev.sh deploy` — port-forward: `./scripts/k8s-dev.sh forward`
- **Prod:** [SECRETS.md](docs/SECRETS.md) + [DEPLOYMENT.md](docs/DEPLOYMENT.md)

After port-forward:

| Service | URL |
|---------|-----|
| GraphiQL | http://localhost:8081/graphiql |
| Health | http://localhost:8081/actuator/health |
| Grafana | http://localhost:3000 |
| Kafka UI | http://localhost:8080 |
| Prometheus | http://localhost:9090 |
| OpenSearch Dashboards | http://localhost:5601 |

## Project layout

```
src/main/kotlin/.../service/
├── config/                      # Exception handlers, tracing
├── example/                     # Example domain (entity, repo, service, REST, GraphQL)
src/main/resources/
├── application.yaml
├── application-k8s.yaml
├── graphql/example/schema.graphqls
└── db/migration/                # Flyway
infra/helm/stack/
├── config/images.yaml           # Image versions (single source of truth)
├── local.yaml                   # Local K8s: credentials inline
├── dev.yaml                     # Dev: credentials from Secrets
└── prod.yaml                    # Prod: Secrets, HPA, backups
```

**Add a domain:** Copy `example` package and `graphql/example`, add Flyway migration. See [DOMAINS.md](docs/DOMAINS.md).

## Docs

- [DEPLOYMENT.md](docs/DEPLOYMENT.md) — Helm install (local, dev, prod)
- [DOCKER.md](docs/DOCKER.md) — Docker Compose local development
- [DOMAINS.md](docs/DOMAINS.md) — Domain structure and adding new domains
- [SECRETS.md](docs/SECRETS.md) — Kubernetes secrets for dev/prod
- [OBSERVABILITY.md](docs/OBSERVABILITY.md) — Logs, metrics, traces (OpenSearch, Prometheus, Tempo)
