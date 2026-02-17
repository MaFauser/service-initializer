# Deployment Guide

Quick reference for deploying the service across different environments.

## Environment Overview

| Environment | Purpose | Infrastructure | App Location | Resources |
|-------------|---------|----------------|--------------|-----------|
| **Local** | Developer laptop | Docker Compose | Host | Minimal |
| **Dev** | Shared dev/staging | Cloud K8s cluster | Inside K8s | Moderate |
| **Prod** | Production | Cloud K8s cluster | Inside K8s | High |

### Cloud Provider Guides

- **[Oracle Kubernetes Engine (OKE)](./docs/ORACLE.md)** – Local access, ServiceAccount setup, CI/CD deployment

---

## Local Development (Your Laptop)

**Goal**: Run infrastructure via Docker Compose, app on host for fast iteration.

```bash
# Start all services
docker-compose up -d

# Run your app
./gradlew bootRun

# App connects to localhost:5432, localhost:6379, etc.
```

**Cleanup:**
```bash
docker-compose down -v
```

---

## Local Kubernetes (Docker Desktop)

**Goal**: Run the full stack in Kubernetes on your laptop using Docker Desktop's built-in cluster.

### Quick start (script)

```bash
./scripts/k8s-local.sh up
```

This will: build the app image, deploy with Helm, wait for pods, and port-forward all services. Press Ctrl+C to stop port-forwards.

Other commands:
- `./scripts/k8s-local.sh deploy` - Deploy only (no port-forward)
- `./scripts/k8s-local.sh forward` - Port-forward only (after deploy)
- `./scripts/k8s-local.sh stop` - Stop port-forwards
- `./scripts/k8s-local.sh down` - Uninstall everything

### Manual setup

#### 1. Enable Kubernetes in Docker Desktop

- Open **Docker Desktop** → **Settings** → **Kubernetes**
- Check **Enable Kubernetes**
- Click **Apply & Restart**
- Wait until the status shows "Kubernetes running" (green)

#### 2. Install Helm (if needed)

```bash
# macOS (Homebrew)
brew install helm

# Or download from https://helm.sh/docs/intro/install/
```

#### 3. Build the app image

```bash
./gradlew bootBuildImage --imageName=service:dev
```

#### 4. Deploy with Helm (local = raw credentials in values-local.yaml; no Secrets)

```bash
helm install dev ./helm/stack \
  -f ./helm/stack/config/shared.yaml \
  -f ./helm/stack/values.yaml \
  -f ./helm/stack/values-local.yaml \
  --set application.image.repository=service \
  --set application.image.tag=dev \
  --set application.image.pullPolicy=IfNotPresent \
  --namespace development \
  --create-namespace
```

#### 5. Wait for pods to be ready

```bash
kubectl get pods -n development -w
# Press Ctrl+C when all pods show "Running"
```

#### 6. Access services (port-forward)

```bash
# In separate terminals, or run in background with &
kubectl port-forward -n development svc/dev-stack-app 8081:8081       # App
kubectl port-forward -n development svc/dev-stack-grafana 3000:3000   # Grafana
kubectl port-forward -n development svc/dev-stack-kafka-ui 8080:8080  # Kafka UI
```

- **App**: http://localhost:8081
- **Grafana**: http://localhost:3000 (admin/admin)
- **Kafka UI**: http://localhost:8080

### Cleanup

```bash
helm uninstall dev -n development
kubectl delete namespace development
```

### Troubleshooting

**Image pull error for the app**: Docker Desktop K8s shares the local Docker daemon; `IfNotPresent` uses the image you built. If it still fails, try:
```bash
# Use kind instead (loads images into the cluster)
brew install kind
kind create cluster
kind load docker-image service:dev
# Then run the helm install again
```

**Pods stuck in Pending**: Check `kubectl describe pod <pod-name> -n development` for resource limits. Docker Desktop defaults may need more CPU/memory in Settings.

---

## Development Cluster (Shared Staging)

**Goal**: Full deployment in a shared Kubernetes cluster for team testing. Dev uses **Secrets** only (no raw passwords in values).

### Create Secrets (once)

Create the two required Secrets before the first deploy. **Full guide (key names, verification, CI/CD):** [Creating Secrets (docs/SECRETS.md)](docs/SECRETS.md).

```bash
kubectl create namespace development --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic dev-postgresql-credentials \
  --from-literal=username=service --from-literal=password=<secret> -n development
kubectl create secret generic dev-grafana-credentials \
  --from-literal=adminUser=admin --from-literal=adminPassword=<secret> -n development
```

### Initial Deployment
```bash
helm install dev ./helm/stack \
  -f ./helm/stack/config/shared.yaml \
  -f ./helm/stack/values.yaml \
  -f ./helm/stack/values-dev.yaml \
  --namespace development \
  --create-namespace

# Check deployment status
kubectl get pods -n development
helm status dev -n development
```

### Access Services
```bash
# Port-forward for local access
kubectl port-forward -n development svc/dev-stack-kafka-ui 8080:8080
kubectl port-forward -n development svc/dev-stack-grafana 3000:3000
kubectl port-forward -n development svc/dev-stack-app 8081:8081

# Or create Ingress resources (recommended for shared cluster)
```

### Update App Version
```bash
# Build and push new image
./gradlew bootBuildImage --imageName=your-registry/service:v1.2.0
docker push your-registry/service:v1.2.0

# Update deployment
helm upgrade dev ./helm/stack \
  -f ./helm/stack/config/shared.yaml \
  -f ./helm/stack/values.yaml \
  -f ./helm/stack/values-dev.yaml \
  --set application.image.tag=v1.2.0 \
  --namespace development
```

### Cleanup
```bash
helm uninstall dev -n development
kubectl delete namespace development
```

---

## Production Cluster

**Goal**: Secure, scalable production deployment. Prod uses **Secrets** only (no raw passwords in values).

### Prerequisites

Create the two required Secrets before the first deploy. **Full guide:** [Creating Secrets (docs/SECRETS.md)](docs/SECRETS.md).

```bash
kubectl create namespace production

# Create Secrets (names must match values-prod.yaml: existingSecret)
kubectl create secret generic prod-postgresql-credentials \
  --from-literal=username=service --from-literal=password=$DB_PASSWORD -n production
kubectl create secret generic prod-grafana-credentials \
  --from-literal=adminUser=admin --from-literal=adminPassword=$GRAFANA_PASSWORD -n production
```

### Initial Deployment
```bash
helm install prod ./helm/stack \
  -f ./helm/stack/config/shared.yaml \
  -f ./helm/stack/values.yaml \
  -f ./helm/stack/values-prod.yaml \
  --set application.image.tag=v1.0.0 \
  --namespace production

# Verify deployment
kubectl get pods -n production
kubectl get pvc -n production
helm status prod -n production
```

### Production Updates (Rolling Deployment)
```bash
# Update to new version
helm upgrade prod ./helm/stack \
  -f ./helm/stack/config/shared.yaml \
  -f ./helm/stack/values.yaml \
  -f ./helm/stack/values-prod.yaml \
  --set application.image.tag=v1.1.0 \
  --namespace production

# Watch rollout
kubectl rollout status deployment/prod-stack-app -n production

# Rollback if needed
helm rollback prod -n production
```

### Monitoring
```bash
# Access Grafana (via LoadBalancer or Ingress in production)
kubectl get svc -n production

# View logs
kubectl logs -f deployment/prod-stack-app -n production

# Check metrics
kubectl top pods -n production
```

---

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Deploy to Dev

on:
  push:
    branches: [develop]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build and Push Image
        run: |
          ./gradlew bootBuildImage --imageName=${{ secrets.REGISTRY }}/service:${{ github.sha }}
          docker push ${{ secrets.REGISTRY }}/service:${{ github.sha }}

      - name: Deploy to Dev
        run: |
          helm upgrade --install dev ./helm/stack \
            -f ./helm/stack/values-dev.yaml \
            --set application.image.tag=${{ github.sha }} \
            --namespace development
```

### ArgoCD
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: service-prod
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/service-initializer
    targetRevision: main
    path: helm/stack
    helm:
      valueFiles:
        - values-prod.yaml
      parameters:
        - name: application.image.tag
          value: "1.0.0"
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

---

## Production Checklist

Before going to production, ensure:

- [ ] Use **Kubernetes Secrets** for passwords: create `prod-postgresql-credentials` and `prod-grafana-credentials` before install (see [Creating Secrets](docs/SECRETS.md)).
- [ ] Configure **Ingress** with TLS: set `ingress.enabled: true`, `ingress.hosts`, and `ingress.tls` (requires an Ingress controller in the cluster).
- [ ] Enable **PostgreSQL backups**: set `postgresql.backup.enabled: true` (CronJob writes to PVC; sync to object storage separately). See [Backups](#backups) below.
- [ ] Configure **monitoring alerts** in Grafana (and runbooks).
- [ ] Enable **network policies**: set `networkPolicy.enabled: true` in values-prod.
- [ ] Use **managed services** if possible for HA (RDS, ElastiCache, MSK, OCI equivalents).
- [ ] Configure **resource quotas** and **limits** (already in values-prod).
- [ ] Set up **logging** aggregation (OpenSearch/Fluent Bit are optional in the chart).
- [ ] Enable **HPA**: set `application.autoscaling.enabled: true` in values-prod.
- [ ] Test **disaster recovery** procedures (restore from backup).
- [ ] Document **runbooks** for common issues.

For a full picture of what the chart provides, see **[Production Readiness](docs/PRODUCTION-READINESS.md)**.

### Backups

**PostgreSQL:** The chart includes an optional CronJob. Set `postgresql.backup.enabled: true` in values-prod; it runs `pg_dump` on a schedule (default daily at 2 AM) and writes to a PVC (retains 7 days). Sync that PVC to object storage with a separate job, or use [Velero](https://velero.io/) for volume backup.

**Redis / Kafka:** No backup CronJob in the chart; use managed services with backup support or add your own strategy.

---

## Troubleshooting

### Pods not starting
```bash
kubectl get pods -n <namespace>
kubectl describe pod <pod-name> -n <namespace>
kubectl logs <pod-name> -n <namespace>
```

### Check Helm release
```bash
helm list -n <namespace>
helm status <release-name> -n <namespace>
helm get values <release-name> -n <namespace>
```

### Database connection issues
```bash
# Test from within cluster
kubectl run -it --rm debug --image=postgres:16-alpine --restart=Never -- \
  psql -h <service-name> -U service -d servicedb
```

### View all resources
```bash
kubectl get all,pvc,configmap,secret -n <namespace>
```

---

## Quick Commands Reference

```bash
# Install
helm install <name> ./helm/stack -f ./helm/stack/values-<env>.yaml --namespace <ns> --create-namespace

# Upgrade
helm upgrade <name> ./helm/stack -f ./helm/stack/values-<env>.yaml --namespace <ns>

# Uninstall
helm uninstall <name> --namespace <ns>

# Port-forward
kubectl port-forward -n <ns> svc/<service-name> <local-port>:<service-port>

# Logs
kubectl logs -f -n <ns> deployment/<deployment-name>

# Shell into pod
kubectl exec -it -n <ns> <pod-name> -- /bin/sh

# Restart deployment
kubectl rollout restart -n <ns> deployment/<deployment-name>
```
