# Docker Environment Setup

Local development uses Docker Compose for infrastructure dependencies.

## Services

- **PostgreSQL** (port 5432) — Main database
- **pgAdmin** (port 5050) — PostgreSQL admin UI (admin@local.dev / admin)
- **Redis** (port 6379) — Caching layer
- **Kafka** (port 9092) — Message broker (KRaft mode, no Zookeeper)
- **Kafka UI** (port 8080) — Web interface for Kafka management

## Quick Start

```bash
docker compose up -d
```

Start specific services only:
```bash
docker compose up -d postgres redis kafka
```

View logs:
```bash
docker compose logs -f
```

Stop all services:
```bash
docker compose down
```

Stop and remove volumes (deletes data):
```bash
docker compose down -v
```

## Database Connection

```
Host:     localhost
Port:     5432
Database: servicedb
Username: service
Password: service123
```

## Health Checks

All services include health checks:
```bash
docker compose ps
```

## Kafka Topics

Create topics via Kafka UI (http://localhost:8080) or CLI:
```bash
docker compose exec kafka kafka-topics --create \
  --topic your-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

## Troubleshooting

### Kafka not starting
- Wait 30 seconds for initialization
- Check logs: `docker compose logs kafka`

### Port conflicts
- Stop conflicting services or change ports in `docker-compose.yml`

### Reset everything
```bash
docker compose down -v
docker compose up -d
```

## Notes

- All data is persisted in Docker volumes
- Kafka uses KRaft mode (no Zookeeper required)
- The app runs on the host (`make run`), not in Docker
