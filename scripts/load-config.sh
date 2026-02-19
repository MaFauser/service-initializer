#!/bin/bash
# Generate .env for docker-compose from helm values (images.yaml + local.yaml).
# Requires: yq (https://github.com/mikefarah/yq)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
IMAGES="$PROJECT_ROOT/helm/stack/config/images.yaml"
LOCAL="$PROJECT_ROOT/helm/stack/local.yaml"
VALUES="$PROJECT_ROOT/helm/stack/values.yaml"
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

cat > "$OUT" << EOF
# Auto-generated from config/images.yaml + local.yaml — do not edit

# PostgreSQL
POSTGRES_IMAGE=$(img '.postgresql')
POSTGRES_DB=$(yq '.postgresql.auth.database' "$LOCAL")
POSTGRES_USER=$(yq '.postgresql.auth.username' "$LOCAL")
POSTGRES_PASSWORD=$(yq '.postgresql.auth.password' "$LOCAL")

# Redis
REDIS_IMAGE=$(img '.redis')

# Kafka
KAFKA_IMAGE=$(img '.kafka')
KAFKA_CLUSTER_ID=$(yq '.kafka.config.clusterId' "$VALUES")

# Kafka UI
KAFKA_UI_IMAGE=$(img '.kafkaUi')

# Grafana
GRAFANA_IMAGE=$(img '.grafana')
GF_ADMIN_USER=$(yq '.grafana.auth.adminUser' "$LOCAL")
GF_ADMIN_PASSWORD=$(yq '.grafana.auth.adminPassword' "$LOCAL")

# Tempo
TEMPO_IMAGE=$(img '.tempo')

# Prometheus
PROMETHEUS_IMAGE=$(img '.prometheus')
EOF

echo "Generated $OUT"
