#!/bin/bash
# Generate .env from helm values (images.yaml + local.yaml + values.yaml).
# Used by: docker-compose (auto-reads .env) and Makefile (include .env / export).
# Single source of truth: infra/helm/stack/config/images.yaml, infra/helm/stack/local.yaml, infra/helm/stack/values.yaml.
# Requires: yq (https://github.com/mikefarah/yq)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
IMAGES="$PROJECT_ROOT/infra/helm/stack/config/images.yaml"
LOCAL="$PROJECT_ROOT/infra/helm/stack/local.yaml"
VALUES="$PROJECT_ROOT/infra/helm/stack/values.yaml"
OUT="$PROJECT_ROOT/.env"

if ! command -v yq &>/dev/null; then
  echo "Error: yq is required. Install: brew install yq"
  exit 1
fi

for f in "$IMAGES" "$LOCAL" "$VALUES"; do
  if [ ! -f "$f" ]; then
    echo "Error: $f not found"
    exit 1
  fi
done

img() { yq "$1.image.repository + \":\" + $1.image.tag" "$IMAGES"; }

PG_DB=$(yq '.postgresql.auth.database' "$LOCAL")
PG_USER=$(yq '.postgresql.auth.username' "$LOCAL")
PG_PASS=$(yq '.postgresql.auth.password' "$LOCAL")
PG_PORT=$(yq '.postgresql.service.port' "$VALUES")
REDIS_PORT=$(yq '.redis.service.port' "$VALUES")
KAFKA_PORT=$(yq '.kafka.service.port' "$VALUES")
OTLP_PORT=$(yq '.tempo.service.ports.otlpHttp' "$VALUES")

cat > "$OUT" << EOF
# Auto-generated from helm values — do not edit. Run: make config

# Docker images
POSTGRES_IMAGE=$(img '.postgresql')
REDIS_IMAGE=$(img '.redis')
KAFKA_IMAGE=$(img '.kafka')
KAFKA_UI_IMAGE=$(img '.kafkaUi')
GRAFANA_IMAGE=$(img '.grafana')
TEMPO_IMAGE=$(img '.tempo')
PROMETHEUS_IMAGE=$(img '.prometheus')

# PostgreSQL
POSTGRES_DB=${PG_DB}
POSTGRES_USER=${PG_USER}
POSTGRES_PASSWORD=${PG_PASS}
POSTGRES_URL=jdbc:postgresql://localhost:${PG_PORT}/${PG_DB}

# Kafka
KAFKA_CLUSTER_ID=$(yq '.kafka.config.clusterId' "$VALUES")

# Grafana
GF_ADMIN_USER=$(yq '.grafana.auth.adminUser' "$LOCAL")
GF_ADMIN_PASSWORD=$(yq '.grafana.auth.adminPassword' "$LOCAL")

# Redis / Kafka / Tracing
REDIS_HOST=localhost
REDIS_PORT=${REDIS_PORT}
KAFKA_BOOTSTRAP_SERVERS=localhost:${KAFKA_PORT}
OTLP_TRACES_ENDPOINT=http://localhost:${OTLP_PORT}/v1/traces
EOF

PGADMIN_DIR="$PROJECT_ROOT/infra/docker/pgadmin"
mkdir -p "$PGADMIN_DIR"
cat > "$PGADMIN_DIR/servers.json" << SERVERS
{
  "Servers": {
    "1": {
      "Name": "PostgreSQL (${PG_DB})",
      "Group": "Servers",
      "Host": "postgres",
      "Port": ${PG_PORT},
      "MaintenanceDB": "${PG_DB}",
      "Username": "${PG_USER}",
      "SSLMode": "prefer"
    }
  }
}
SERVERS

echo "Generated $OUT, pgadmin/servers.json"
