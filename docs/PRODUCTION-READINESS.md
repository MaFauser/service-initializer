# Production Readiness

This document summarizes what this project provides for production and what you must add for **large-scale, production-grade** deployment.

---

## Short Answer

- **Is it production-wise?** The chart now includes **production-oriented options**: Secrets (`existingSecret` for Postgres and app datasource), Ingress + TLS, HPA, NetworkPolicy, and a **PostgreSQL backup CronJob**. Enable and configure them in `values-prod.yaml` (or via `--set`). For large scale, consider managed DB/Kafka.
- **Do we have backups?** **PostgreSQL:** Yes – optional CronJob (`postgresql.backup.enabled`) runs `pg_dump` to a PVC daily; sync that PVC to object storage separately or use Velero. Redis/Kafka: no automated backup in the chart; use managed services or add your own.
- **Strong enough for large-scale production?** With secrets, ingress, HPA, network policies, and Postgres backups enabled, you have a solid production setup. For very large scale, use managed databases and message brokers (RDS, MSK, OCI equivalents).

---

## What This Project Gives You Today

| Area | Status |
|------|--------|
| **App** | 3 replicas in prod, health probes, resource limits |
| **Kafka** | `replicationFactor: 3` in prod, persistence |
| **PostgreSQL / Redis / Kafka** | Single instance each, persistence (PVCs), no HA |
| **Observability** | Grafana, Prometheus, Tempo, OpenSearch (optional), Fluent Bit |
| **Secrets** | Optional: `postgresql.auth.existingSecret` and `application.datasource.existingSecret` – use K8s Secrets in prod |
| **Ingress / TLS** | Optional: `ingress.enabled` with `hosts` and `tls` (requires Ingress controller) |
| **Backups** | **PostgreSQL:** Optional CronJob (`postgresql.backup.enabled`) – pg_dump to PVC; retain 7 days |
| **HPA** | Optional: `application.autoscaling.enabled` – scale app by CPU |
| **Network policies** | Optional: `networkPolicy.enabled` – restrict app ↔ postgres/redis/kafka/tempo |

Prod defaults: `values-prod.yaml` enables backup, autoscaling, and networkPolicy; set `existingSecret` and `ingress` when ready.

---

## What’s In the Chart (Enable in Production)

### 1. Secrets

- **PostgreSQL:** Set `postgresql.auth.existingSecret` to the name of a Secret containing `username` and `password` (or custom keys via `usernameKey` / `passwordKey`). The Postgres deployment and backup CronJob use it.
- **App datasource:** Set `application.datasource.existingSecret` so the app gets `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` from that Secret. Can be the same Secret as Postgres.

Create the Secret before install:  
`kubectl create secret generic prod-postgresql-credentials --from-literal=username=service --from-literal=password=<secret> -n production`

### 2. Ingress and TLS

- Set `ingress.enabled: true`, `ingress.hosts`, and optionally `ingress.tls` (secretName + hosts). Requires an Ingress controller (e.g. NGINX, Traefik) in the cluster. For TLS, use cert-manager (annotate `cert-manager.io/cluster-issuer`) or provide a TLS Secret.

### 3. HPA

- Set `application.autoscaling.enabled: true`. Adjust `minReplicas`, `maxReplicas`, and `targetCPUUtilizationPercentage` in values-prod.

### 4. Network policies

- Set `networkPolicy.enabled: true` to restrict app egress to postgres/redis/kafka/tempo and postgres ingress to app (and backup job) only.

### 5. PostgreSQL backups

- Set `postgresql.backup.enabled: true`. A CronJob runs `pg_dump` on the schedule (default daily at 2 AM), writes to a PVC, keeps 7 days. Sync the backup PVC to object storage with a separate job or use [Velero](https://velero.io/) for volume backup.

---

## What’s Still Your Responsibility

### Backups (Redis / Kafka)

- **Redis:** No backup CronJob in the chart; use RDB/replication or a managed cache with backups.
- **Kafka:** Replication and retention are configured; for critical data consider a managed service with backup/snapshot.

### HA for data layer

- **PostgreSQL, Redis, Kafka:** Single instance each. For HA, use managed services (RDS, ElastiCache, MSK, OCI equivalents) or multi-replica topologies.

### Alerting

- Grafana/Prometheus are present; **configure alerts** (e.g. PagerDuty, Slack) and runbooks.

---

## Summary Table

| Need | In this repo? | How to use |
|------|----------------|------------|
| Backups (PostgreSQL) | **Yes** (CronJob) | `postgresql.backup.enabled: true`; backups to PVC; sync to object storage separately |
| Backups (Redis / Kafka) | No | Use managed services or add your own |
| Secrets (K8s) | **Yes** | `postgresql.auth.existingSecret`, `application.datasource.existingSecret` |
| Ingress + TLS | **Yes** | `ingress.enabled: true`, set hosts and tls |
| HA for Postgres/Redis/Kafka | No (single instance) | Use managed services or multi-replica HA |
| HPA | **Yes** | `application.autoscaling.enabled: true` |
| Network policies | **Yes** | `networkPolicy.enabled: true` |
| Alerting | Stack only | Configure alerts in Grafana and runbooks |

---

## Conclusion

The chart is **production-oriented**: enable Secrets, Ingress, HPA, NetworkPolicy, and Postgres backup in `values-prod.yaml` (and set `existingSecret` and ingress hosts/tls). For very large scale or HA, use managed databases and message brokers. The [Production Checklist](../DEPLOYMENT.md#production-checklist) in DEPLOYMENT.md is the canonical list.
