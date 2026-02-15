# Helm Chart Templates Structure

Organized by component:

```
templates/
├── _helpers.tpl          # Shared template helpers
├── application/          # Spring Boot app
│   ├── deployment.yaml
│   └── service.yaml
├── postgresql/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── redis/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── kafka/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── kafka-ui/
│   ├── deployment.yaml
│   └── service.yaml
├── grafana/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   ├── configmap-datasources.yaml
│   ├── configmap-dashboards-provider.yaml
│   └── configmap-dashboards.yaml
├── tempo/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   └── configmap.yaml
├── prometheus/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   └── configmap.yaml
├── opensearch/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   └── secret.yaml
├── opensearch-dashboards/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── provision-index-pattern-job.yaml  # Helm hook: creates kubernetes-logs-* index pattern
└── fluent-bit/
    ├── configmap.yaml
    ├── daemonset.yaml
    └── rbac.yaml
```
