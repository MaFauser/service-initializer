# Helm Chart for Service Application

This Helm chart deploys a complete Spring Boot service stack with PostgreSQL, Redis, Kafka, and observability tools (Grafana, Tempo, Prometheus).

## Prerequisites

- Kubernetes cluster (local or cloud)
  - **Local options**: Docker Desktop, Minikube, kind, k3s/k3d
  - **Cloud options**: AWS EKS, GCP GKE, Azure AKS
- Helm 3.x installed
- kubectl configured to access your cluster

## Two-Tier Environment Strategy

| Environment | Use Case | Cluster Type | App Deployment |
|-------------|----------|--------------|----------------|
| **dev** | Shared dev/staging | Cloud K8s cluster | Deployed in cluster |
| **prod** | Production | Cloud K8s cluster | Deployed in cluster |

**Local development** uses Docker Compose (`docker-compose up -d` + `./gradlew bootRun`), not Helm.

## Quick Start

### 1. Development Cluster (Shared Staging)

```bash
# Deploy everything including the app
helm install dev ./helm/stack \
  -f ./helm/stack/values-dev.yaml \
  --namespace development \
  --create-namespace
```

### 2. Production Cluster

```bash
# Deploy with production settings
helm install prod ./helm/stack \
  -f ./helm/stack/values-prod.yaml \
  --set postgresql.auth.password=$DB_PASSWORD \
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

- **`values.yaml`** - Base configuration (defaults)
- **`values-dev.yaml`** - **Shared dev/staging** cluster (moderate resources, includes app deployment)
- **`values-prod.yaml`** - **Production** cluster (high resources, HA settings, includes app deployment)

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
helm install dev ./helm/stack -f ./helm/stack/values-dev.yaml --namespace development --create-namespace

# Production cluster
helm install prod ./helm/stack -f ./helm/stack/values-prod.yaml --namespace production --create-namespace
```

### Upgrade
```bash
# After changing values
helm upgrade myservice ./helm/stack -f ./helm/stack/values-dev.yaml
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
│  │  - Grafana (3000)                     │      │
│  │  - Tempo (4317/4318)                  │      │
│  │  - Prometheus (9090)                  │      │
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

1. **Use Secrets**: Store passwords in Kubernetes Secrets, not in values files
   ```bash
   kubectl create secret generic db-secret --from-literal=password=yourpassword
   ```

2. **Use Managed Services**: Consider using cloud-managed services:
   - AWS RDS (PostgreSQL)
   - AWS ElastiCache (Redis)
   - AWS MSK (Kafka)
   - AWS Managed Grafana/Prometheus

3. **Resource Limits**: Adjust based on actual usage and monitoring

4. **Backups**: Set up automated backups for databases

5. **Monitoring**: Integrate with your existing monitoring stack

6. **Security**: Enable authentication, network policies, and RBAC

## License

This Helm chart is part of the Service Initializer project.
