#!/bin/bash
# Generate .env from helm/stack/config/shared.yaml for docker-compose.
# Run before: docker compose up
# Requires: yq (https://github.com/mikefarah/yq)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG="$PROJECT_ROOT/helm/stack/config/shared.yaml"
OUT="$PROJECT_ROOT/.env"

if ! command -v yq &>/dev/null; then
  echo "Error: yq is required. Install: brew install yq (macOS) or see https://github.com/mikefarah/yq"
  exit 1
fi

if [ ! -f "$CONFIG" ]; then
  echo "Error: Config not found at $CONFIG"
  exit 1
fi

cat > "$OUT" << EOF
# Auto-generated from helm/stack/config/shared.yaml - do not edit manually

# PostgreSQL
POSTGRES_IMAGE=$(yq '.postgresql.image.repository + ":" + .postgresql.image.tag' "$CONFIG")
POSTGRES_DB=$(yq '.postgresql.auth.database' "$CONFIG")
POSTGRES_USER=$(yq '.postgresql.auth.username' "$CONFIG")
POSTGRES_PASSWORD=$(yq '.postgresql.auth.password' "$CONFIG")

# Redis
REDIS_IMAGE=$(yq '.redis.image.repository + ":" + .redis.image.tag' "$CONFIG")

# Kafka
KAFKA_IMAGE=$(yq '.kafka.image.repository + ":" + .kafka.image.tag' "$CONFIG")
KAFKA_CLUSTER_ID=$(yq '.kafka.config.clusterId' "$CONFIG")

# Kafka UI
KAFKA_UI_IMAGE=$(yq '.kafkaUi.image.repository + ":" + .kafkaUi.image.tag' "$CONFIG")

# Grafana
GRAFANA_IMAGE=$(yq '.grafana.image.repository + ":" + .grafana.image.tag' "$CONFIG")
GF_ADMIN_USER=$(yq '.grafana.auth.adminUser' "$CONFIG")
GF_ADMIN_PASSWORD=$(yq '.grafana.auth.adminPassword' "$CONFIG")

# Tempo
TEMPO_IMAGE=$(yq '.tempo.image.repository + ":" + .tempo.image.tag' "$CONFIG")

# Prometheus
PROMETHEUS_IMAGE=$(yq '.prometheus.image.repository + ":" + .prometheus.image.tag' "$CONFIG")
APP_TARGET=host.docker.internal:8081
EOF

# Generate prometheus config for Docker (substitute __APP_TARGET__)
PROM_SRC="$PROJECT_ROOT/helm/stack/config/prometheus.yml"
PROM_OUT="$PROJECT_ROOT/docker/prometheus.generated.yml"
mkdir -p "$(dirname "$PROM_OUT")"
sed 's/__APP_TARGET__/host.docker.internal:8081/g' "$PROM_SRC" > "$PROM_OUT"
echo "Generated $PROM_OUT"
