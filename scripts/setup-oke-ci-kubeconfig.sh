#!/bin/bash
# Create a CI-ready kubeconfig using a Kubernetes ServiceAccount token (OKE).
# Run this locally with .kubeconfig-dev.b64 (OCI exec auth) - outputs base64 for KUBECONFIG_DEV secret.
# See: https://docs.oracle.com/en-us/iaas/Content/ContEng/Tasks/contengaddingserviceaccttoken.htm

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
KUBECONFIG_B64="$PROJECT_ROOT/.kubeconfig-dev.b64"
SA_NAME="kubeconfig-sa"
BINDING_NAME="add-on-cluster-admin"
SECRET_NAME="oke-kubeconfig-sa-token"
SA_NAMESPACE="kube-system"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Setting up OKE CI kubeconfig (ServiceAccount token)...${NC}"
echo ""

# Load kubeconfig
if [ ! -f "$KUBECONFIG_B64" ]; then
    echo -e "${RED}Error: .kubeconfig-dev.b64 not found. Use your OCI exec kubeconfig first.${NC}"
    exit 1
fi
export KUBECONFIG="$(mktemp -t kubeconfig-ci-setup-XXXXXX)"
trap "rm -f $KUBECONFIG" EXIT
base64 -d < "$KUBECONFIG_B64" > "$KUBECONFIG"

echo -e "${YELLOW}[1/6] Creating ServiceAccount...${NC}"
kubectl -n $SA_NAMESPACE create serviceaccount $SA_NAME --dry-run=client -o yaml | kubectl apply -f -
echo -e "  ${GREEN}✓${NC}"

echo -e "${YELLOW}[2/6] Creating ClusterRoleBinding (cluster-admin)...${NC}"
kubectl create clusterrolebinding $BINDING_NAME \
    --clusterrole=cluster-admin \
    --serviceaccount=$SA_NAMESPACE:$SA_NAME \
    --dry-run=client -o yaml | kubectl apply -f -
echo -e "  ${GREEN}✓${NC}"

echo -e "${YELLOW}[3/6] Creating token Secret...${NC}"
kubectl apply -f "$SCRIPT_DIR/oke-ci-sa-token.yaml"
echo -e "  ${GREEN}✓${NC}"

echo -e "${YELLOW}[4/6] Waiting for secret to be populated...${NC}"
sleep 3
for i in 1 2 3 4 5; do
    if kubectl -n $SA_NAMESPACE get secret $SECRET_NAME -o jsonpath='{.data.token}' &>/dev/null; then
        break
    fi
    echo "  Waiting... ($i/5)"
    sleep 2
done

echo -e "${YELLOW}[5/6] Building CI kubeconfig...${NC}"
TOKEN=$(kubectl -n $SA_NAMESPACE get secret $SECRET_NAME -o jsonpath='{.data.token}' | base64 -d)
CA_DATA=$(kubectl -n $SA_NAMESPACE get secret $SECRET_NAME -o jsonpath='{.data.ca\.crt}')
SERVER=$(kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}')

# Build kubeconfig with token (no exec)
CI_KUBECONFIG=$(mktemp -t kubeconfig-ci-XXXXXX)
cat > "$CI_KUBECONFIG" << EOF
apiVersion: v1
kind: Config
clusters:
- cluster:
    certificate-authority-data: ${CA_DATA}
    server: ${SERVER}
  name: cluster
contexts:
- context:
    cluster: cluster
    user: ${SA_NAME}
  name: context
current-context: context
users:
- name: ${SA_NAME}
  user:
    token: ${TOKEN}
EOF

echo -e "${YELLOW}[6/6] Base64 encoding...${NC}"
CI_B64=$(base64 < "$CI_KUBECONFIG" | tr -d '\n')
rm -f "$CI_KUBECONFIG"

echo ""
echo -e "${GREEN}✓ CI kubeconfig ready!${NC}"
echo ""
echo "Add this to your GitHub repository secret KUBECONFIG_DEV:"
echo ""
echo "---"
echo "$CI_B64"
echo "---"
echo ""
echo "Or save to file:"
echo "  echo '$CI_B64' | base64 -d > .kubeconfig-ci.yaml"
echo ""
