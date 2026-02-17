#!/bin/bash
# Local Kubernetes: deploy and port-forward all services (Docker Desktop K8s)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
HELM_CHART="$PROJECT_ROOT/helm/stack"
NAMESPACE="development"
RELEASE_NAME="dev"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_usage() {
    echo "Usage: $0 <action>"
    echo ""
    echo "Actions:"
    echo "  up          - Build image, deploy with Helm, wait for pods, start port-forwards"
    echo "  deploy      - Build image and deploy/upgrade with Helm"
    echo "  forward     - Port-forward all services (app, grafana, kafka-ui, prometheus)"
    echo "  stop        - Kill port-forwards"
    echo "  down        - Uninstall Helm release"
    echo ""
    exit 1
}

check_cluster() {
    if ! kubectl cluster-info &>/dev/null; then
        echo -e "${RED}Error: Cannot connect to Kubernetes cluster.${NC}"
        echo "Make sure Docker Desktop is running with Kubernetes enabled."
        exit 1
    fi
}

build_image() {
    echo -e "${YELLOW}Building app image...${NC}"
    "$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" bootBuildImage --imageName=service:dev -q
    echo -e "${GREEN}✓ Image built${NC}"
}

deploy() {
    check_cluster
    build_image

    echo -e "${YELLOW}Deploying with Helm...${NC}"
    helm upgrade --install $RELEASE_NAME "$HELM_CHART" \
        -f "$HELM_CHART/config/shared.yaml" \
        -f "$HELM_CHART/values.yaml" \
        -f "$HELM_CHART/values-local.yaml" \
        --set application.image.repository=service \
        --set application.image.tag=dev \
        --set application.image.pullPolicy=IfNotPresent \
        --namespace $NAMESPACE \
        --create-namespace

    echo -e "${GREEN}✓ Deployed. Waiting for pods...${NC}"
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=$RELEASE_NAME -n $NAMESPACE --timeout=300s 2>/dev/null || true
    kubectl get pods -n $NAMESPACE
}

forward() {
    check_cluster

    PIDS=()
    kill_forwards() {
        if [ ${#PIDS[@]} -gt 0 ]; then
            kill "${PIDS[@]}" 2>/dev/null || true
        fi
    }
    trap kill_forwards EXIT

    echo -e "${YELLOW}Starting port-forwards...${NC}"
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-app 8081:8081 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-grafana 3000:3000 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-kafka-ui 8080:8080 &
    PIDS+=($!)
    kubectl port-forward -n $NAMESPACE svc/${RELEASE_NAME}-stack-prometheus 9090:9090 &
    PIDS+=($!)

    sleep 1
    echo ""
    echo -e "${GREEN}✓ Port-forwards active. Press Ctrl+C to stop.${NC}"
    echo ""
    echo "  App:       http://localhost:8081"
    echo "  Grafana:   http://localhost:3000 (admin/admin)"
    echo "  Kafka UI:  http://localhost:8080"
    echo "  Prometheus: http://localhost:9090"
    echo ""
    wait
}

stop_forwards() {
    echo -e "${YELLOW}Stopping port-forwards...${NC}"
    pkill -f "kubectl port-forward.*$NAMESPACE" 2>/dev/null || true
    echo -e "${GREEN}✓ Done${NC}"
}

down() {
    echo -e "${YELLOW}Uninstalling Helm release...${NC}"
    helm uninstall $RELEASE_NAME --namespace $NAMESPACE 2>/dev/null || true
    kubectl delete namespace $NAMESPACE --ignore-not-found --timeout=60s 2>/dev/null || true
    echo -e "${GREEN}✓ Done${NC}"
}

if [ "$#" -lt 1 ]; then
    print_usage
fi

cd "$PROJECT_ROOT"

case $1 in
    up)
        deploy
        echo ""
        forward
        ;;
    deploy)
        deploy
        ;;
    forward)
        forward
        ;;
    stop)
        stop_forwards
        ;;
    down)
        down
        ;;
    *)
        echo -e "${RED}Error: Unknown action '$1'${NC}"
        print_usage
        ;;
esac
