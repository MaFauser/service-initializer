# Observability: Logs vs Metrics vs Traces

| Tool | What it stores | Where to view |
|------|----------------|---------------|
| **Prometheus** | **Metrics** (request counts, latencies, JVM, CPU) | Grafana dashboards, Prometheus UI |
| **Tempo** | **Traces** (distributed spans) | Grafana → Explore → Tempo, Tempo Traces dashboard |
| **OpenSearch** | **Logs** (pod stdout: request logs, errors, debug) | OpenSearch Dashboards → Discover, or Grafana → Explore → OpenSearch |

**Dashboards** (Service folder in Grafana): Spring Boot, Tempo Traces, Observability Overview

**End-to-end tracing:** Logs include `traceId` and `spanId` (from OpenTelemetry). Grafana links them:
- **Trace → logs:** In Tempo (trace view), click "Logs for this span" to see related logs in OpenSearch
- **Logs → trace:** In Grafana Explore with OpenSearch, click a `traceId` value to open the trace in Tempo

**Prometheus does NOT store logs.** It stores numeric metrics (counters, gauges, histograms).

---

## Viewing logs

### OpenSearch Dashboards (Discover)

1. **Port-forward** (if using remote cluster): `./scripts/k8s-dev.sh forward`
2. **Open OpenSearch Dashboards** → http://localhost:5601
3. **Discover** → The `kubernetes-logs-*` index pattern is created automatically on deploy
4. **Query examples:**
   - Filter by `kubernetes.namespace_name: development` – all logs in dev namespace
   - Filter by `kubernetes.pod_name: dev-stack-app*` – logs from your Spring Boot app
   - Full-text search: `error` – app logs containing "error"
   - Filter by `kubernetes.container_name: app` – logs from containers named "app"

### kubectl (streaming)

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

Request logs appear in the app logs (stdout) and in your log aggregator (OpenSearch, etc.).

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

## OpenSearch (included in Helm stack)

OpenSearch, OpenSearch Dashboards, and Fluent Bit are deployed by the Helm chart. Fluent Bit runs as a DaemonSet, collecting pod logs from each node and sending them to OpenSearch. After deploy:

- **OpenSearch Dashboards** → http://localhost:5601 (after port-forward) → **Discover** to search logs
- The index pattern is `kubernetes-logs-*` (Logstash-style date suffix)
- With security disabled (default for dev): no login. With security enabled: admin / configurable password

### Index pattern and Discover columns

- **Where it’s configured:** The index pattern is created by the Helm post-install job `infra/helm/stack/templates/opensearch-dashboards/provision-index-pattern-job.yaml`. The pattern title and time field come from `opensearchDashboards.indexPattern` and `timeFieldName: @timestamp` in that job (see `helm/stack/values.yaml` and env-specific overrides).
- **Fields like message, traceId:** They come from the **index mapping** (Fluent Bit parses JSON and lifts fields; see `infra/helm/stack/templates/fluent-bit/configmap.yaml`). OpenSearch Dashboards discovers them from the data; no extra config is needed for them to exist.
- **Making fields indexable (so you can add them as columns):** If some `log_processed.*` fields show a “not indexed” warning in Discover and you can’t add them as columns, OpenSearch is using dynamic mapping and didn’t index those sub-fields. The stack applies an **index template** so they are explicitly indexed:
  - **Config:** `infra/helm/stack/templates/opensearch/index-template-configmap.yaml` (mapping for `log_processed.message`, `log_processed.traceId`, `log_processed.spanId`, `log_processed.level`, `log_processed.logger_name`, etc.) and the Job `infra/helm/stack/templates/opensearch/provision-index-template-job.yaml` (post-install hook that applies the template to OpenSearch).
  - **Effect:** New indices matching `kubernetes-logs-*` get these mappings, so the fields are indexed and usable as Discover columns. **Existing indices** keep their old mapping; to get the new mapping for current data you can delete the existing log indices (data loss) or wait for the next day/rollover so new indices use the template.
- **Default columns in Discover:** The provision job only sets the index pattern title and time field. After the index template is in place, add **message**, **traceId**, **spanId**, **level**, **logger_name**, **kubernetes.pod_name**, etc. in Discover via the “Add field” / column picker.
