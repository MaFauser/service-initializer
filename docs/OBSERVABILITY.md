# Observability: Logs vs Metrics vs Traces

| Tool | What it stores | Where to view |
|------|----------------|---------------|
| **Prometheus** | **Metrics** (request counts, latencies, JVM, CPU) | Grafana dashboards, Prometheus UI |
| **Tempo** | **Traces** (distributed spans) | Grafana → Service → Tempo Traces, or Explore → Tempo |

**Dashboards** (Service folder): Spring Boot, Tempo Traces, Observability Overview
| **Logs** | **Application logs** (stdout: request logs, errors, debug) | `kubectl logs`, or Loki if added |

**Prometheus does NOT store logs.** It stores numeric metrics (counters, gauges, histograms).

---

## Viewing logs

### Kubernetes (dev/prod)

```bash
# Stream app logs
kubectl logs -f deployment/dev-stack-app -n development

# Or use the script
./scripts/k8s-dev.sh logs
```

### Local (Docker Compose)

Logs go to stdout. When you run `./gradlew bootRun`, they appear in the terminal.

---

## Request logging

All HTTP requests are logged automatically:

```
GET /examples 200 45ms
POST /examples 201 12ms
GET /graphql 200 89ms
```

This is done by `RequestLoggingFilter` and appears in the app logs.

---

## Viewing traces in Grafana

1. **Ensure the app sends traces to Tempo** – `application.yaml` configures OTLP endpoint `http://localhost:4318/v1/traces` for local dev. Restart the app after config changes.
2. **Open Grafana** → http://localhost:3000 (admin/admin)
3. **Tempo Traces dashboard** – Table (left) lists traces; Trace detail (right) shows spans. Click a trace ID ("Show trace") to view it in the detail panel.
4. **Explore** → Select **Tempo** → Use TraceQL, e.g. `{ resource.service.name="service" }` to search.
5. **Time range** – Pick "Last 15 minutes" or a range that includes your requests.

---

## Troubleshooting: Tempo shows "No data"

If Prometheus dashboards show requests but Tempo shows none:

1. **Restart Grafana** after datasource changes: `docker compose restart grafana`
2. **Verify app → Tempo**: App sends to `localhost:4318`. Ensure Docker Compose is up and Tempo exposes port 4318.
3. **Generate traces**: Hit a non-actuator endpoint (e.g. http://localhost:8081/graphiql, run a query). Actuator endpoints are excluded from tracing.
4. **Check Tempo logs**: `docker logs service-tempo` for ingest errors.
5. **Widen time range**: Use "Last 15 minutes" or "Last 1 hour" in the dashboard.
6. **Explore → Tempo**: Try a manual search with TraceQL `{}` (all traces) to see if any data exists.
7. **Grafana version**: Pinned to `grafana:12.3.3`. Run `docker compose pull grafana && docker compose up -d grafana` to apply.

---

## Troubleshooting: Latency (p95) shows no data

The Observability Overview dashboard uses `histogram_quantile()` on `http_server_requests_seconds_bucket`. Spring Boot exposes histogram buckets only when enabled:

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

This is configured in `application.yaml`. Restart the app and generate some HTTP traffic; p95 latency will appear after Prometheus scrapes.

---

## Adding Loki (log aggregation)

To query logs in Grafana (like Prometheus for metrics), add [Loki](https://grafana.com/oss/loki/):

- Deploy Loki (Helm chart or Docker)
- Add Loki as a datasource in Grafana
- Configure log shipping (Promtail, Fluent Bit, or Grafana Agent) to send container logs to Loki

Then you can query logs in Grafana → Explore → Loki.
