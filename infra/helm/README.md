# Helm Chart

Deploys Spring Boot + PostgreSQL, Redis, Kafka, Grafana, Tempo, Prometheus, OpenSearch, Fluent Bit.

## Files

| File | What |
|------|------|
| `values.yaml` | Chart defaults (ports, services, persistence, resources) — auto-loaded by Helm |
| `config/images.yaml` | Image repos + tags — single source of truth for versions |
| `local.yaml` | Local K8s: credentials inline, minimal resources |
| `dev.yaml` | Dev: credentials from Secrets, moderate resources |
| `prod.yaml` | Prod: credentials from Secrets, HPA, backups, network policies |

## Install

```bash
# Local
helm install dev ./infra/helm/stack \
  -f ./infra/helm/stack/config/images.yaml \
  -f ./infra/helm/stack/local.yaml \
  --namespace development --create-namespace

# Dev (create Secrets first — see docs/SECRETS.md)
helm install dev ./infra/helm/stack \
  -f ./infra/helm/stack/config/images.yaml \
  -f ./infra/helm/stack/dev.yaml \
  --namespace development --create-namespace

# Prod
helm install prod ./infra/helm/stack \
  -f ./infra/helm/stack/config/images.yaml \
  -f ./infra/helm/stack/prod.yaml \
  --namespace production --create-namespace
```

## Upgrade / Uninstall

```bash
helm upgrade dev ./infra/helm/stack \
  -f ./infra/helm/stack/config/images.yaml \
  -f ./infra/helm/stack/dev.yaml \
  --namespace development

helm uninstall dev --namespace development
# Delete data: kubectl delete pvc -l app.kubernetes.io/instance=dev -n development
```

## Port-forward

```bash
# Or use: ./scripts/k8s-dev.sh forward
kubectl port-forward svc/dev-stack-grafana 3000:3000 -n development
kubectl port-forward svc/dev-stack-opensearch-dashboards 5601:5601 -n development
kubectl port-forward svc/dev-stack-kafka-ui 8080:8080 -n development
kubectl port-forward svc/dev-stack-prometheus 9090:9090 -n development
```

## Toggle services

```bash
helm install dev ./infra/helm/stack \
  -f ./infra/helm/stack/config/images.yaml \
  -f ./infra/helm/stack/local.yaml \
  --set kafka.enabled=false \
  --set kafkaUi.enabled=false
```

## Bump versions

Edit `config/images.yaml`, then `helm upgrade`.

## Production

See `prod.yaml` and [Production Readiness](../docs/PRODUCTION-READINESS.md):

- **Secrets:** `postgresql.auth.existingSecret`, `application.datasource.existingSecret`, `grafana.auth.existingSecret`
- **Ingress + TLS:** `ingress.enabled: true`
- **HPA:** `application.autoscaling.enabled: true`
- **Network policies:** `networkPolicy.enabled: true`
- **Postgres backups:** `postgresql.backup.enabled: true`
