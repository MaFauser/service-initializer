#!/bin/bash
# Validate CI kubeconfig (ServiceAccount token format) before pasting into GitHub KUBECONFIG_DEV.
# Usage:
#   ./scripts/validate-ci-kubeconfig.sh < base64-file
#   echo "YOUR_BASE64" | ./scripts/validate-ci-kubeconfig.sh
#   ./scripts/validate-ci-kubeconfig.sh .kubeconfig-ci.b64

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ -n "$1" ] && [ -f "$1" ]; then
    B64=$(cat "$1")
else
    B64=$(cat)
fi

# Remove whitespace/newlines (GitHub secrets can have them)
B64=$(echo "$B64" | tr -d ' \n\r\t')

echo -e "${YELLOW}Validating kubeconfig...${NC}"

# Decode
DECODED=$(echo "$B64" | base64 -d 2>/dev/null) || {
    echo -e "${RED}Error: Invalid base64. Cannot decode.${NC}"
    echo "Make sure you're passing a single base64 string (no extra newlines or spaces)."
    exit 1
}

# Check for double-encoding (decoded result looks like base64)
if echo "$DECODED" | head -1 | grep -qE '^[A-Za-z0-9+/]{20,}=*$'; then
    echo -e "${RED}Error: Likely double base64 encoded.${NC}"
    echo "The decoded content looks like base64. Encode only once: base64 -i config.yaml | tr -d '\n'"
    exit 1
fi

# Check YAML structure
if ! echo "$DECODED" | head -5 | grep -q "apiVersion:"; then
    echo -e "${RED}Error: Decoded content doesn't look like kubeconfig (missing apiVersion).${NC}"
    echo "First 5 lines of decoded content:"
    echo "$DECODED" | head -5 | sed 's/^/  /'
    exit 1
fi

if ! echo "$DECODED" | grep -q "kind: Config"; then
    echo -e "${YELLOW}Warning: 'kind: Config' not found (might be valid YAML anyway).${NC}"
fi

echo -e "${GREEN}✓ Base64 decodes to valid kubeconfig structure${NC}"
echo ""
echo "Decoded preview (first 15 lines):"
echo "---"
echo "$DECODED" | head -15 | sed 's/^/  /'
echo "---"
echo ""

# Test kubectl if available
if command -v kubectl &>/dev/null; then
    echo -e "${YELLOW}Testing kubectl cluster-info...${NC}"
    TMP=$(mktemp -t kubeconfig-validate-XXXXXX)
    echo "$DECODED" > "$TMP"
    trap "rm -f $TMP" EXIT
    if KUBECONFIG="$TMP" kubectl cluster-info --request-timeout=15 &>/dev/null; then
        echo -e "${GREEN}✓ kubectl can connect to cluster${NC}"
    else
        echo -e "${YELLOW}⚠ kubectl cluster-info failed (cluster may be unreachable from this machine)${NC}"
        echo "  The kubeconfig format is OK - CI might still work if the cluster is reachable from GitHub runners."
    fi
else
    echo "Tip: Run 'kubectl cluster-info' with this kubeconfig to verify connectivity."
fi
