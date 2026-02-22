#!/bin/bash
# Remote dev cluster: connect using base64 kubeconfig, port-forward, inspect

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
KUBECONFIG_B64="$PROJECT_ROOT/.kubeconfig-dev.b64"
NAMESPACE="development"
RELEASE_NAME="dev"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ensure_kubeconfig() {
    if [ ! -f "$KUBECONFIG_B64" ]; then
        echo -e "${RED}Error: Base64 kubeconfig not found.${NC}"
        echo "Create: kubectl config view --flatten --minify | base64 > .kubeconfig-dev.b64"
        exit 1
    fi
    export KUBECONFIG="$(mktemp -t kubeconfig-dev-XXXXXX)"
    if ! base64 -d < "$KUBECONFIG_B64" > "$KUBECONFIG" 2>/dev/null; then
        echo -e "${RED}Error: Invalid base64 in .kubeconfig-dev.b64${NC}"
        exit 1
    fi
}

print_usage() {
    echo "Usage: $0 <action> [args...]"
    echo ""
    echo "Actions:"
    echo "  forward        - Port-forward services (app, grafana, kafka-ui, prometheus, postgres, opensearch, opensearch-dashboards)"
    echo "  stop           - Kill port-forwards"
    echo "  pods           - Show pods in development namespace"
    echo "  logs           - Tail app logs"
    echo "  status         - Helm status"
    echo "  describe <pod>  - Describe a pod (e.g. dev-stack-postgresql-xxx)"
    echo "  delete-pod <pod> - Delete a pod (e.g. dev-stack-app-xxx); use scale-app 0 to stop app from recreating"
    echo "  scale-app [0|1] - Scale app deployment to 0 (stop) or 1 (run)"
    echo "  fix-postgres-password - Reset DB user password from Secret (fixes 'password authentication failed')"
    echo "  recreate-db    - Delete Postgres PVC and pod; DB re-initializes from Secret (data loss)"
    echo "  events         - Show recent namespace events"
    echo "  clear-stuck    - Delete failed pods to release volumes (ImageInspectError, CrashLoopBackOff, etc.)"
    echo "  deploy         - Helm upgrade, then start port-forwards (use after CI push or for local deploy)"
    echo "  run            - Spawn shell with KUBECONFIG set (run arbitrary kubectl commands)"
    echo ""
    echo "Setup (once):"
    echo "  Save base64 kubeconfig to .kubeconfig-dev.b64"
    echo "  kubectl config view --flatten --minify | base64 > .kubeconfig-dev.b64"
    echo ""
    exit 1
}

forward() {
    ensure_kubeconfig

    PIDS=()
    kill_forwards() {
        if [ ${#PIDS[@]} -gt 0 ]; then
            kill "${PIDS[@]}" 2>/dev/null || true
        fi
    }
    trap "kill_forwards; rm -f $KUBECONFIG" EXIT

    echo -e "${YELLOW}Port-forwarding to dev cluster...${NC}"
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-app 8081:8081 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-postgresql 5432:5432 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-grafana 3000:3000 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-kafka-ui 8080:8080 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-prometheus 9090:9090 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-opensearch 9200:9200 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-opensearch-dashboards 5601:5601 &
    PIDS+=($!)

    sleep 1
    echo ""
    echo -e "${GREEN}✓ Port-forwards active. Press Ctrl+C to stop.${NC}"
    echo ""
    echo "  App:                http://localhost:8081"
    echo "  PostgreSQL:         localhost:5432 (use in DBeaver; user/password from your dev Secret)"
    echo "  Grafana:            http://localhost:3000 (admin/admin)"
    echo "  Kafka UI:           http://localhost:8080"
    echo "  Prometheus:         http://localhost:9090"
    echo "  OpenSearch:         http://localhost:9200"
    echo "  OpenSearch Dashboards: http://localhost:5601 (no auth when disableSecurity=true)"
    echo ""
    wait
}

stop_forwards() {
    echo -e "${YELLOW}Stopping port-forwards...${NC}"
    pkill -f "kubectl port-forward.*$NAMESPACE" 2>/dev/null || true
    echo -e "${GREEN}✓ Done${NC}"
}

pods() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    kubectl get pods -n $NAMESPACE
}

logs() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    kubectl logs -f -n $NAMESPACE deployment/${RELEASE_NAME}-stack-app
}

status() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    helm status $RELEASE_NAME -n $NAMESPACE
    echo ""
    kubectl get pods -n $NAMESPACE
}

describe_pod() {
    if [ -z "${1:-}" ]; then
        echo -e "${RED}Error: Pod name required. Example: $0 describe dev-stack-postgresql-7d99975454-tfjzx${NC}"
        exit 1
    fi
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    kubectl describe pod "$1" -n $NAMESPACE
}

events() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp' | tail -40
}

deploy() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    local image_repo="${DEPLOY_IMAGE_REPO:-ghcr.io/mafauser/service-initializer}"
    local image_tag="${2:-dev-latest}"
    echo -e "${YELLOW}Helm upgrade (image: $image_repo:$image_tag)...${NC}"
    helm upgrade --install $RELEASE_NAME "$PROJECT_ROOT/infra/helm/stack" \
      -f "$PROJECT_ROOT/infra/helm/stack/config/images.yaml" \
      -f "$PROJECT_ROOT/infra/helm/stack/dev.yaml" \
      --set application.image.repository="$image_repo" \
      --set application.image.tag="$image_tag" \
      --namespace $NAMESPACE \
      --create-namespace
    echo -e "${GREEN}Deploy done. Waiting for Grafana rollout...${NC}"
    kubectl rollout status deployment/${RELEASE_NAME}-stack-grafana -n $NAMESPACE --timeout=120s 2>/dev/null || true
    echo -e "${GREEN}Starting port-forwards...${NC}"
    echo ""
    forward
}

run_shell() {
    ensure_kubeconfig
    echo -e "${GREEN}KUBECONFIG set. Run kubectl commands (e.g. kubectl get pods -n $NAMESPACE). Exit to leave.${NC}"
    export KUBECONFIG
    trap "rm -f $KUBECONFIG" EXIT
    exec "${SHELL:-/bin/bash}"
}

delete_pod() {
    if [ -z "${1:-}" ]; then
        echo -e "${RED}Error: Pod name required. Example: $0 delete-pod dev-stack-app-744dd99b9c-k47vw${NC}"
        exit 1
    fi
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    echo -e "${YELLOW}Deleting pod $1...${NC}"
    kubectl delete pod "$1" -n $NAMESPACE --grace-period=0 --force 2>/dev/null || true
    echo -e "${GREEN}Done. If the deployment has replicas >= 1, a new pod will be created. To stop the app: $0 scale-app 0${NC}"
}

scale_app() {
    local replicas="${1:-1}"
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    echo -e "${YELLOW}Scaling ${RELEASE_NAME}-stack-app to $replicas replicas...${NC}"
    kubectl scale deployment ${RELEASE_NAME}-stack-app -n $NAMESPACE --replicas="$replicas"
    echo -e "${GREEN}Done.${NC}"
}

fix_postgres_password() {
    local secret_name="${RELEASE_NAME}-postgresql-credentials"
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    echo -e "${YELLOW}Resetting Postgres user 'service' password from Secret $secret_name...${NC}"
    local pass
    pass=$(kubectl get secret "$secret_name" -n $NAMESPACE -o jsonpath='{.data.password}' 2>/dev/null | base64 -d 2>/dev/null) || true
    if [ -z "$pass" ]; then
        echo -e "${RED}Error: Could not read password from Secret $secret_name (namespace: $NAMESPACE).${NC}"
        echo "Create the Secret first; see docs/SECRETS.md"
        exit 1
    fi
    local escaped
    escaped=$(echo "$pass" | sed "s/'/''/g")
    kubectl exec -n $NAMESPACE deployment/${RELEASE_NAME}-stack-postgresql -- \
        psql -U postgres -d servicedb -c "ALTER USER service PASSWORD '$escaped';" || {
        echo -e "${RED}Error: ALTER USER failed. Is Postgres running? Try: $0 pods${NC}"
        exit 1
    }
    echo -e "${GREEN}Password updated. Restarting app...${NC}"
    kubectl rollout restart deployment/${RELEASE_NAME}-stack-app -n $NAMESPACE
    echo -e "${GREEN}Done. App should start successfully; check: $0 logs${NC}"
}

recreate_db() {
    local pvc_name="${RELEASE_NAME}-stack-postgresql"
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    echo -e "${YELLOW}Recreating Postgres (data loss). Steps: scale app + Postgres to 0, delete PVC, helm upgrade (recreates PVC), scale app to 1.${NC}"
    echo -e "${YELLOW}Scaling app to 0...${NC}"
    kubectl scale deployment ${RELEASE_NAME}-stack-app -n $NAMESPACE --replicas=0
    echo -e "${YELLOW}Scaling Postgres to 0...${NC}"
    kubectl scale deployment ${RELEASE_NAME}-stack-postgresql -n $NAMESPACE --replicas=0
    echo -e "${YELLOW}Waiting for Postgres pod to release PVC...${NC}"
    sleep 5
    echo -e "${YELLOW}Deleting Postgres PVC...${NC}"
    kubectl delete pvc -n $NAMESPACE "$pvc_name" --ignore-not-found=true 2>/dev/null || true
    echo -e "${YELLOW}Helm upgrade (recreates PVC and Postgres deployment)...${NC}"
    local image_repo="${DEPLOY_IMAGE_REPO:-ghcr.io/mafauser/service-initializer}"
    local image_tag="${2:-dev-latest}"
    helm upgrade --install $RELEASE_NAME "$PROJECT_ROOT/infra/helm/stack" \
      -f "$PROJECT_ROOT/infra/helm/stack/config/images.yaml" \
      -f "$PROJECT_ROOT/infra/helm/stack/dev.yaml" \
      --set application.image.repository="$image_repo" \
      --set application.image.tag="$image_tag" \
      --namespace $NAMESPACE \
      --create-namespace
    echo -e "${YELLOW}Waiting for Postgres to be ready...${NC}"
    kubectl rollout status deployment/${RELEASE_NAME}-stack-postgresql -n $NAMESPACE --timeout=120s
    echo -e "${YELLOW}Scaling app to 1...${NC}"
    kubectl scale deployment ${RELEASE_NAME}-stack-app -n $NAMESPACE --replicas=1
    echo -e "${GREEN}Done. DB re-initialized from Secret. Check: $0 logs${NC}"
}

clear_stuck() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    echo -e "${YELLOW}Scaling down ReplicaSets and deleting failed pods (ImageInspectError, ImagePullBackOff, ErrImagePull, CrashLoopBackOff, Error)...${NC}"
    local pods
    pods=$(kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | awk '$3 ~ /ImageInspectError|ImagePullBackOff|ErrImagePull|CrashLoopBackOff|Error/ {print $1}')
    if [ -z "$pods" ]; then
        echo -e "${GREEN}No stuck pods found.${NC}"
        return 0
    fi
    for pod in $pods; do
        rs=$(kubectl get pod "$pod" -n $NAMESPACE -o jsonpath='{.metadata.ownerReferences[?(@.kind=="ReplicaSet")].name}' 2>/dev/null || true)
        if [ -n "$rs" ]; then
            echo "  Scale down $rs, delete $pod"
            kubectl scale rs "$rs" -n $NAMESPACE --replicas=0 2>/dev/null || true
        else
            echo "  Deleting $pod"
        fi
        kubectl delete pod "$pod" -n $NAMESPACE --grace-period=0 2>/dev/null || true
    done
    echo -e "${GREEN}Done. New pods should attach volumes and start. Run 'pods' to check.${NC}"
}

if [ "$#" -lt 1 ]; then
    print_usage
fi

cd "$PROJECT_ROOT"

case $1 in
    forward)
        forward
        ;;
    stop)
        stop_forwards
        ;;
    pods)
        pods
        ;;
    logs)
        logs
        ;;
    status)
        status
        ;;
    describe)
        describe_pod "${2:-}"
        ;;
    delete-pod)
        delete_pod "${2:-}"
        ;;
    scale-app)
        scale_app "${2:-1}"
        ;;
    fix-postgres-password)
        fix_postgres_password
        ;;
    recreate-db)
        recreate_db
        ;;
    events)
        events
        ;;
    clear-stuck)
        clear_stuck
        ;;
    deploy)
        deploy "$@"
        ;;
    run)
        run_shell
        ;;
    *)
        echo -e "${RED}Error: Unknown action '$1'${NC}"
        print_usage
        ;;
esac
