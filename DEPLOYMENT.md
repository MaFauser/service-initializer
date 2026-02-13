# Deployment Guide

Quick reference for deploying the service across different environments.

## Environment Overview

| Environment | Purpose | Infrastructure | App Location | Resources |
|-------------|---------|----------------|--------------|-----------|
| **Local** | Developer laptop | Kubernetes (Docker Desktop/Minikube) | Outside K8s | Minimal |
| **Dev** | Shared dev/staging | Cloud K8s cluster | Inside K8s | Moderate |
| **Prod** | Production | Cloud K8s cluster | Inside K8s | High |

---

## Local Development (Your Laptop)

**Goal**: Run infrastructure in Kubernetes, app outside for fast iteration.

### Option 1: Docker Compose (Fastest)
```bash
# Start all services
docker-compose up -d

# Run your app
./gradlew bootRun

# App connects to localhost:5432, localhost:6379, etc.
```

### Option 2: Helm on Local Kubernetes (Production-like)
```bash
# Install infrastructure only
helm install local ./helm/service-chart -f ./helm/service-chart/values-local.yaml

# Port-forward Kafka UI (optional)
kubectl port-forward svc/local-service-chart-kafka-ui 8080:8080 &

# Run your app with K8s profile
./gradlew bootRun --args='--spring.profiles.active=k8s'

# Update service names in application-k8s.yaml to match "local-service-chart-*"
```

**Cleanup:**
```bash
# Docker Compose
docker-compose down -v

# Helm
helm uninstall local
kubectl delete pvc -l app.kubernetes.io/instance=local
```

---

## Development Cluster (Shared Staging)

**Goal**: Full deployment in a shared Kubernetes cluster for team testing.

### Initial Deployment
```bash
# Deploy everything (infrastructure + app)
helm install dev ./helm/service-chart \
  -f ./helm/service-chart/values-dev.yaml \
  --namespace development \
  --create-namespace

# Check deployment status
kubectl get pods -n development
helm status dev -n development
```

### Access Services
```bash
# Port-forward for local access
kubectl port-forward -n development svc/dev-service-chart-kafka-ui 8080:8080
kubectl port-forward -n development svc/dev-service-chart-grafana 3000:3000
kubectl port-forward -n development svc/dev-service-chart-app 8081:8081

# Or create Ingress resources (recommended for shared cluster)
```

### Update App Version
```bash
# Build and push new image
./gradlew bootBuildImage --imageName=your-registry/service:v1.2.0
docker push your-registry/service:v1.2.0

# Update deployment
helm upgrade dev ./helm/service-chart \
  -f ./helm/service-chart/values-dev.yaml \
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

**Goal**: Secure, scalable production deployment.

### Prerequisites
```bash
# Create production namespace
kubectl create namespace production

# Create secrets (don't use plain text in values files!)
kubectl create secret generic db-secret \
  --from-literal=password=$DB_PASSWORD \
  --namespace production

kubectl create secret generic app-secret \
  --from-literal=database-password=$DB_PASSWORD \
  --from-literal=redis-password=$REDIS_PASSWORD \
  --namespace production
```

### Initial Deployment
```bash
# Deploy with production settings
helm install prod ./helm/service-chart \
  -f ./helm/service-chart/values-prod.yaml \
  --set postgresql.auth.password=$DB_PASSWORD \
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
helm upgrade prod ./helm/service-chart \
  -f ./helm/service-chart/values-prod.yaml \
  --set application.image.tag=v1.1.0 \
  --namespace production

# Watch rollout
kubectl rollout status deployment/prod-service-chart-app -n production

# Rollback if needed
helm rollback prod -n production
```

### Monitoring
```bash
# Access Grafana (via LoadBalancer or Ingress in production)
kubectl get svc -n production

# View logs
kubectl logs -f deployment/prod-service-chart-app -n production

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
          helm upgrade --install dev ./helm/service-chart \
            -f ./helm/service-chart/values-dev.yaml \
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
    path: helm/service-chart
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

- [ ] Use **Kubernetes Secrets** for passwords (not values files)
- [ ] Configure **Ingress** with TLS for external access
- [ ] Set up **backups** for PostgreSQL, Redis, Kafka
- [ ] Configure **monitoring alerts** in Grafana
- [ ] Enable **network policies** for security
- [ ] Use **managed services** if possible (RDS, ElastiCache, MSK)
- [ ] Configure **resource quotas** and **limits**
- [ ] Set up **logging** aggregation (ELK, Loki, CloudWatch)
- [ ] Configure **horizontal pod autoscaling** (HPA)
- [ ] Test **disaster recovery** procedures
- [ ] Document **runbooks** for common issues

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
helm install <name> ./helm/service-chart -f ./helm/service-chart/values-<env>.yaml --namespace <ns> --create-namespace

# Upgrade
helm upgrade <name> ./helm/service-chart -f ./helm/service-chart/values-<env>.yaml --namespace <ns>

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
