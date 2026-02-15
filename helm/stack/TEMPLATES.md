# Helm Chart Templates Structure

Templates are organized by component:

```
templates/
├── _helpers.tpl          # Shared helpers (labels, fullname, grafana checksum)
├── application/          # Spring Boot app (optional)
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
│   ├── configmap-datasources.yaml   # Prometheus, Tempo, OpenSearch (when enabled)
│   ├── configmap-dashboards-provider.yaml
│   └── configmap-dashboards.yaml    # Dashboards from dashboards/*.json (symlink to grafana/dashboards)
├── tempo/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   └── configmap.yaml
├── prometheus/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   └── configmap.yaml               # Scrape config; app target when application.enabled
├── opensearch/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   └── secret.yaml                  # When disableSecurity=false
├── opensearch-dashboards/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── provision-index-pattern-job.yaml  # post-install hook: creates kubernetes-logs-* index pattern
└── fluent-bit/
    ├── configmap.yaml               # Tail + K8s filter + logstash_json parser → OpenSearch
    ├── daemonset.yaml
    └── rbac.yaml
```
