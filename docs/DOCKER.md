# Docker Environment Setup

This document describes the Docker Compose setup for local development.

> **💡 Kubernetes Alternative**: For production deployments or if you prefer Kubernetes, see the [Helm Chart documentation](../infra/helm/README.md) for deploying to any Kubernetes cluster (local or cloud).

## Single Source of Truth

Docker Compose and the Helm chart share config from **`infra/helm/stack/config/images.yaml`** (images, credentials, Kafka cluster ID). Before first use, run:

```bash
./scripts/load-config.sh
```

Requires [yq](https://github.com/mikefarah/yq). This generates `.env` and `infra/docker/prometheus.generated.yml`.

## Services

The `docker-compose.yml` file includes the following services:

### Core Services
- **PostgreSQL** (port 5432) - Main database
- **Redis** (port 6379) - Caching layer
- **Kafka** (port 9092) - Message broker (KRaft mode, no Zookeeper needed)

### UI & Monitoring
- **pgAdmin** (port 5050) - PostgreSQL admin UI; pre-configured to connect to the `postgres` service (login: admin@local.dev / admin)
- **Kafka UI** (port 8080) - Web interface for Kafka management
- **Grafana** (port 3000) - Observability dashboards (admin/admin)
- **Tempo** (port 4317/4318) - Distributed tracing backend
- **Prometheus** (port 9090) - Metrics collection

## Quick Start

### Start all services:
```bash
docker-compose up -d
```

### Start specific services:
```bash
docker-compose up -d postgres redis kafka
```

### View logs:
```bash
docker-compose logs -f
```

### Stop all services:
```bash
docker-compose down
```

### Stop and remove volumes (⚠️ deletes data):
```bash
docker-compose down -v
```

## Service URLs

- **pgAdmin**: http://localhost:5050 — Login with **admin@local.dev** / **admin**. The server **PostgreSQL (servicedb)** is pre-configured (connection is loaded on first startup when the pgAdmin config DB is created). If the server prompts for a password, re-run `make config` and restart the pgadmin container (`docker compose restart pgadmin`).
- **Kafka UI**: http://localhost:8080
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090

## Database Connection

```
Host: localhost
Port: 5432
Database: servicedb
Username: service
Password: service123
```

## Health Checks

All services include health checks. You can verify service health:
```bash
docker-compose ps
```

## Kafka Topics

Create topics via Kafka UI or CLI:
```bash
docker-compose exec kafka kafka-topics --create \
  --topic your-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

## Troubleshooting

### Kafka not starting
- Wait 30 seconds for initialization
- Check logs: `docker-compose logs kafka`

### Port conflicts
- Stop conflicting services or change ports in `docker-compose.yml`

### Reset everything
```bash
docker-compose down -v
docker-compose up -d
```

## Notes

- All data is persisted in Docker volumes
- Kafka uses KRaft mode (no Zookeeper required)
- OpenTelemetry traces are sent to Tempo on port 4318
- Prometheus scrapes Spring Boot actuator metrics on port 8081
