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
    echo -e "${YELLOW}[1/2] Loading kubeconfig...${NC}"
    if [ ! -f "$KUBECONFIG_B64" ]; then
        echo -e "${RED}Error: Base64 kubeconfig not found.${NC}"
        echo ""
        echo "Create the file once:"
        echo "  1. Get your cluster kubeconfig (e.g. from Oracle Cloud OKE, or your cloud provider)"
        echo "  2. Encode it:  kubectl config view --flatten --minify | base64"
        echo "  3. Save to:    .kubeconfig-dev.b64"
        echo ""
        echo "Example:"
        echo "  kubectl config view --flatten --minify | base64 > .kubeconfig-dev.b64"
        echo ""
        exit 1
    fi
    export KUBECONFIG="$(mktemp -t kubeconfig-dev-XXXXXX)"
    echo "  Decoding .kubeconfig-dev.b64..."
    if ! base64 -d < "$KUBECONFIG_B64" > "$KUBECONFIG" 2>/dev/null; then
        echo -e "${RED}Error: Invalid base64 in .kubeconfig-dev.b64${NC}"
        echo "Encode with: kubectl config view --flatten --minify | base64 > .kubeconfig-dev.b64"
        exit 1
    fi
    echo -e "  ${GREEN}✓ Kubeconfig ready${NC}"
}

check_cluster() {
    echo -e "${YELLOW}[2/2] Connecting to cluster...${NC}"
    echo "  (If using OCI exec plugin, this may take a few seconds to fetch token)"
    local err ret
    set +e
    err=$(kubectl cluster-info --request-timeout=30 2>&1)
    ret=$?
    set -e
    if [ $ret -ne 0 ]; then
        echo -e "${RED}Error: Cannot connect to cluster.${NC}"
        echo ""
        echo "$err" | head -10
        echo ""
        echo "Common causes: VPN not connected, cluster unreachable, expired credentials."
        exit 1
    fi
    echo -e "  ${GREEN}✓ Cluster reachable${NC}"
}

print_usage() {
    echo "Usage: $0 <action>"
    echo ""
    echo "Actions:"
    echo "  forward    - Port-forward services (app, grafana, kafka-ui, prometheus)"
    echo "  stop       - Kill port-forwards"
    echo "  pods       - Show pods in development namespace"
    echo "  logs       - Tail app logs"
    echo "  status     - Helm status"
    echo ""
    echo "Setup (once):"
    echo "  Save base64 kubeconfig to .kubeconfig-dev.b64"
    echo "  kubectl config view --flatten --minify | base64 > .kubeconfig-dev.b64"
    echo ""
    exit 1
}

forward() {
    ensure_kubeconfig
    check_cluster

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

pods() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    check_cluster
    kubectl get pods -n $NAMESPACE
}

logs() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    check_cluster
    kubectl logs -f -n $NAMESPACE deployment/${RELEASE_NAME}-stack-app
}

status() {
    ensure_kubeconfig
    trap "rm -f $KUBECONFIG" EXIT
    check_cluster
    helm status $RELEASE_NAME -n $NAMESPACE
    echo ""
    kubectl get pods -n $NAMESPACE
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
    *)
        echo -e "${RED}Error: Unknown action '$1'${NC}"
        print_usage
        ;;
esac
