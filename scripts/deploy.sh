#!/bin/bash
# Deployment helper script for service-initializer

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
HELM_CHART="$PROJECT_ROOT/helm/stack"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_usage() {
    echo "Usage: $0 <environment> <action> [release-name]"
    echo ""
    echo "Environments:"
    echo "  local       - Local Kubernetes (minimal resources)"
    echo "  dev         - Development cluster (moderate resources)"
    echo "  prod        - Production cluster (high resources)"
    echo ""
    echo "Actions:"
    echo "  install     - Install the Helm chart"
    echo "  upgrade     - Upgrade existing installation"
    echo "  uninstall   - Remove the installation"
    echo "  status      - Show installation status"
    echo ""
    echo "Examples:"
    echo "  $0 local install"
    echo "  $0 dev upgrade myapp"
    echo "  $0 prod status production-service"
    exit 1
}

if [ "$#" -lt 2 ]; then
    print_usage
fi

ENVIRONMENT=$1
ACTION=$2
RELEASE_NAME=${3:-"service"}

# Validate environment
case $ENVIRONMENT in
    local|dev|prod)
        VALUES_FILE="$HELM_CHART/values-$ENVIRONMENT.yaml"
        ;;
    *)
        echo -e "${RED}Error: Invalid environment '$ENVIRONMENT'${NC}"
        print_usage
        ;;
esac

# Set namespace based on environment
case $ENVIRONMENT in
    local)
        NAMESPACE="default"
        ;;
    dev)
        NAMESPACE="development"
        ;;
    prod)
        NAMESPACE="production"
        ;;
esac

echo -e "${GREEN}Environment: $ENVIRONMENT${NC}"
echo -e "${GREEN}Release Name: $RELEASE_NAME${NC}"
echo -e "${GREEN}Namespace: $NAMESPACE${NC}"
echo -e "${GREEN}Values File: $VALUES_FILE${NC}"
echo ""

# Execute action
case $ACTION in
    install)
        echo -e "${YELLOW}Installing Helm chart...${NC}"
        if [ "$ENVIRONMENT" != "local" ]; then
            kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
        fi
        helm install $RELEASE_NAME $HELM_CHART \
            -f $HELM_CHART/config/shared.yaml \
            -f $VALUES_FILE \
            --namespace $NAMESPACE
        echo -e "${GREEN}✓ Installation complete!${NC}"
        echo ""
        echo "Run the following to check status:"
        echo "  kubectl get pods -n $NAMESPACE"
        echo "  helm status $RELEASE_NAME -n $NAMESPACE"
        ;;

    upgrade)
        echo -e "${YELLOW}Upgrading Helm chart...${NC}"
        helm upgrade $RELEASE_NAME $HELM_CHART \
            -f $HELM_CHART/config/shared.yaml \
            -f $VALUES_FILE \
            --namespace $NAMESPACE
        echo -e "${GREEN}✓ Upgrade complete!${NC}"
        ;;

    uninstall)
        echo -e "${YELLOW}Uninstalling Helm chart...${NC}"
        helm uninstall $RELEASE_NAME --namespace $NAMESPACE
        echo -e "${GREEN}✓ Uninstall complete!${NC}"
        echo ""
        echo -e "${YELLOW}To delete PVCs (⚠️ deletes data):${NC}"
        echo "  kubectl delete pvc -l app.kubernetes.io/instance=$RELEASE_NAME -n $NAMESPACE"
        ;;

    status)
        echo -e "${YELLOW}Checking status...${NC}"
        helm status $RELEASE_NAME --namespace $NAMESPACE
        echo ""
        kubectl get pods -n $NAMESPACE
        ;;

    *)
        echo -e "${RED}Error: Invalid action '$ACTION'${NC}"
        print_usage
        ;;
esac
