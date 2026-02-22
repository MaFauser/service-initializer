# Config

| File | Purpose |
|------|---------|
| `images.yaml` | Image repos + tags — single source of truth for versions. Used by Helm (`-f config/images.yaml`). |
| `tempo.yaml` | Tempo config. Helm template reads via `.Files.Get`. |
| `prometheus.yml` | Prometheus config. Placeholder `__APP_TARGET__` is substituted by Helm → `{release}-app:8081`. |

**Bumping versions:** Edit `images.yaml`, then redeploy.
