# Helm Chart for Service Application

This Helm chart deploys a complete Spring Boot service stack with PostgreSQL, Redis, Kafka, and observability tools (Grafana, Tempo, Prometheus, OpenSearch, Fluent Bit).

## Prerequisites

- Kubernetes cluster (local or cloud)
  - **Local options**: Docker Desktop, Minikube, kind, k3s/k3d
  - **Cloud options**: AWS EKS, GCP GKE, Azure AKS
- Helm 3.x installed
- kubectl configured to access your cluster

## Values per environment (no defaults/fallbacks)

One values file per environment. Credentials are never defaulted in templates.

| File | Credentials | Use |
|------|-------------|-----|
| **values-local.yaml** | Raw usernames/passwords in the file | Local K8s (e.g. Docker Desktop, kind) |
| **values-dev.yaml** | Secrets only (`existingSecret`); no raw passwords | Dev/staging cluster |
| **values-prod.yaml** | Secrets only (`existingSecret`); no raw passwords | Production cluster |

**Local development** (laptop) often uses Docker Compose (`docker-compose up -d` + `./gradlew bootRun`); for local K8s use `values-local.yaml`.

## Quick Start

Always use: `-f config/shared.yaml -f values.yaml -f values-<env>.yaml`. Images come from `config/shared.yaml`.

### 1. Local (raw credentials in file)

```bash
helm install dev ./helm/stack \
  -f ./helm/stack/config/shared.yaml \
  -f ./helm/stack/values.yaml \
  -f ./helm/stack/values-local.yaml \
  --namespace development \
  --create-namespace
```

### 2. Dev (Secrets; create Secrets before install)

Create the required Secrets first (see **[Creating Secrets](../docs/SECRETS.md)** for full steps and key names):

```bash
kubectl create secret generic dev-postgresql-credentials \
  --from-literal=username=service --from-literal=password=<secret> -n development
kubectl create secret generic dev-grafana-credentials \
  --from-literal=adminUser=admin --from-literal=adminPassword=<secret> -n development

helm install dev ./helm/stack \
  -f ./helm/stack/config/shared.yaml \
  -f ./helm/stack/values.yaml \
  -f ./helm/stack/values-dev.yaml \
  --namespace development \
  --create-namespace
```

### 3. Prod (Secrets; create Secrets before install)

Create the required Secrets first (see **[Creating Secrets](../docs/SECRETS.md)**):

```bash
kubectl create secret generic prod-postgresql-credentials \
  --from-literal=username=service --from-literal=password=<secret> -n production
kubectl create secret generic prod-grafana-credentials \
  --from-literal=adminUser=admin --from-literal=adminPassword=<secret> -n production

helm install prod ./helm/stack \
  -f ./helm/stack/config/shared.yaml \
  -f ./helm/stack/values.yaml \
  -f ./helm/stack/values-prod.yaml \
  --namespace production \
  --create-namespace
```

### 3. Access Services

After installation, you can access services using port-forwarding:

```bash
# Kafka UI
kubectl port-forward svc/myservice-stack-kafka-ui 8080:8080
# Open http://localhost:8080

# Grafana
kubectl port-forward svc/myservice-stack-grafana 3000:3000
# Open http://localhost:3000 (admin/admin)

# Prometheus
kubectl port-forward svc/myservice-stack-prometheus 9090:9090
# Open http://localhost:9090

# OpenSearch Dashboards (logs)
kubectl port-forward svc/myservice-stack-opensearch-dashboards 5601:5601
# Open http://localhost:5601
```

### 4. Connect Your Spring Boot App

Update your `application.yaml` or use environment variables to point to Kubernetes services:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://myservice-stack-postgresql:5432/servicedb
  data:
    redis:
      host: myservice-stack-redis
  kafka:
    bootstrap-servers: myservice-stack-kafka:9092

management:
  otlp:
    tracing:
      endpoint: http://myservice-stack-tempo:4318/v1/traces
```

## Configuration

### Values Files

- **`values.yaml`** - Base structure (no credential defaults; always override with an env file)
- **`config/shared.yaml`** - Images and Kafka config (shared with Docker Compose)
- **`values-local.yaml`** - Local K8s: raw usernames/passwords for Postgres, Grafana, app datasource
- **`values-dev.yaml`** - Dev/staging: Secrets only (`postgresql.auth.existingSecret`, `application.datasource.existingSecret`, `grafana.auth.existingSecret`)
- **`values-prod.yaml`** - Production: Secrets only; backup, HPA, networkPolicy enabled

### Customize Installation

```bash
# Override specific values
helm install myservice ./helm/stack \
  --set postgresql.persistence.size=20Gi \
  --set kafka.resources.limits.memory=3Gi

# Use custom values file
helm install myservice ./helm/stack -f my-custom-values.yaml
```

### Enable/Disable Services

```bash
# Install only database and cache (no Kafka or observability)
helm install myservice ./helm/stack \
  --set kafka.enabled=false \
  --set kafkaUi.enabled=false \
  --set grafana.enabled=false \
  --set tempo.enabled=false \
  --set prometheus.enabled=false
```

## Helm Commands

### Install
```bash
# Development cluster
helm install dev ./helm/stack -f ./helm/stack/config/shared.yaml -f ./helm/stack/values.yaml -f ./helm/stack/values-dev.yaml --namespace development --create-namespace

# Production cluster (create Secrets first)
helm install prod ./helm/stack -f ./helm/stack/config/shared.yaml -f ./helm/stack/values.yaml -f ./helm/stack/values-prod.yaml --namespace production --create-namespace
```

### Upgrade
```bash
# After changing values
helm upgrade myservice ./helm/stack -f ./helm/stack/config/shared.yaml -f ./helm/stack/values.yaml -f ./helm/stack/values-dev.yaml
```

### Uninstall
```bash
# Remove all resources
helm uninstall myservice

# Remove including PVCs (⚠️ deletes data)
helm uninstall myservice
kubectl delete pvc -l app.kubernetes.io/instance=myservice
```

### Debugging
```bash
# Dry run (see what would be deployed)
helm install myservice ./helm/stack --dry-run --debug

# Check current values
helm get values myservice

# Check deployment status
helm status myservice

# View rendered templates
helm template myservice ./helm/stack
```

## Architecture

```
┌─────────────────────────────────────────────────┐
│          Kubernetes Cluster                      │
│                                                  │
│  ┌──────────────┐  ┌──────────────┐            │
│  │ Spring Boot  │  │   Kafka UI   │            │
│  │     App      │  │  (port 8080) │            │
│  └───────┬──────┘  └──────────────┘            │
│          │                                       │
│  ┌───────┼─────────────────────────────┐       │
│  │       ▼                               │       │
│  │  PostgreSQL   Redis      Kafka       │       │
│  │  (port 5432)  (6379)    (9092)       │       │
│  └───────────────────────────────────────┘       │
│                                                  │
│  ┌───────────────────────────────────────┐      │
│  │  Observability Stack                  │      │
│  │  - Grafana (3000), Tempo, Prometheus  │      │
│  │  - OpenSearch (9200), Dashboards      │      │
│  │    (5601), Fluent Bit (logs)         │      │
│  └───────────────────────────────────────┘      │
└─────────────────────────────────────────────────┘
```

## Storage

All stateful services use PersistentVolumeClaims (PVCs):
- PostgreSQL: 10Gi (dev: 5Gi, prod: 50Gi)
- Redis: 5Gi (dev: 2Gi, prod: 20Gi)
- Kafka: 20Gi (dev: 10Gi, prod: 100Gi)
- Grafana: 5Gi
- Tempo: 10Gi (prod: 50Gi)
- Prometheus: 10Gi (prod: 50Gi)
- OpenSearch: 10Gi (prod: 50Gi)

To use a specific storage class:
```bash
helm install myservice ./helm/stack \
  --set global.storageClass=fast-ssd
```

## Environment-Specific Deployments

### Development (Shared Dev/Staging Cluster)
```bash
helm install dev ./helm/stack \
  -f ./helm/stack/values-dev.yaml \
  --namespace development \
  --create-namespace
```
**Characteristics:**
- Moderate resources
- App deployed in cluster (`application.enabled=true`)
- Full observability stack
- Smaller persistence volumes

### Production (Cloud Kubernetes)
```bash
helm install prod ./helm/stack \
  -f ./helm/stack/values-prod.yaml \
  --set postgresql.auth.password=$DB_PASSWORD \
  --namespace production \
  --create-namespace
```
**Characteristics:**
- Production-grade resources
- Multi-replica app deployment
- Large persistence volumes
- Consider using managed services (RDS, ElastiCache, MSK)

## Integration with CI/CD

### GitHub Actions Example
```yaml
- name: Deploy to Kubernetes
  run: |
    helm upgrade --install myservice ./helm/stack \
      -f ./helm/stack/values-prod.yaml \
      --set application.image.tag=${{ github.sha }} \
      --namespace production
```

### ArgoCD
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myservice
spec:
  source:
    repoURL: https://github.com/your-org/service-initializer
    targetRevision: HEAD
    path: helm/stack
    helm:
      valueFiles:
        - values-prod.yaml
  destination:
    server: https://kubernetes.default.svc
    namespace: production
```

## Troubleshooting

### Pods not starting
```bash
# Check pod status
kubectl get pods

# View pod logs
kubectl logs <pod-name>

# Describe pod for events
kubectl describe pod <pod-name>
```

### Storage issues
```bash
# Check PVCs
kubectl get pvc

# Check if storage class exists
kubectl get storageclass
```

### Kafka not connecting
```bash
# Exec into Kafka pod
kubectl exec -it <kafka-pod> -- bash

# Test Kafka
kafka-topics --list --bootstrap-server localhost:9092
```

## Production Considerations

The chart includes production-oriented options (see `values-prod.yaml` and [Production Readiness](../../docs/PRODUCTION-READINESS.md)):

1. **Secrets**: Set `postgresql.auth.existingSecret` and `application.datasource.existingSecret` to use Kubernetes Secrets for DB credentials.
2. **Ingress + TLS**: Set `ingress.enabled: true`, `ingress.hosts`, and `ingress.tls` (requires an Ingress controller).
3. **HPA**: Set `application.autoscaling.enabled: true` to scale the app by CPU.
4. **Network policies**: Set `networkPolicy.enabled: true` to restrict pod-to-pod traffic.
5. **PostgreSQL backups**: Set `postgresql.backup.enabled: true` for a daily pg_dump CronJob to a PVC (sync to object storage separately).
6. **Managed services**: For HA, consider RDS, ElastiCache, MSK, or OCI equivalents instead of in-cluster Postgres/Redis/Kafka.
7. **Monitoring**: Configure alerts in Grafana and runbooks.

## License

This Helm chart is part of the Service Initializer project.
