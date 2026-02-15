# Shared Config (Docker + Helm)

Single source of truth for both Docker Compose and Helm.

| File | Purpose |
|------|---------|
| `shared.yaml` | Images, tags, credentials, Kafka cluster ID. Used by Helm (`-f`) and by `scripts/load-config.sh` for Docker. |
| `tempo.yaml` | Tempo config. Docker mounts directly. Helm template reads via `.Files.Get`. |
| `prometheus.yml` | Prometheus config. Placeholder `__APP_TARGET__` is substituted: Docker → `host.docker.internal:8081`, Helm → `{release}-app:8081`. |

**Changing versions or credentials:** Edit `shared.yaml` only. Then:
- Docker: run `./scripts/load-config.sh` and `docker compose up -d`
- Helm: redeploy (values are passed via `-f config/shared.yaml`)
