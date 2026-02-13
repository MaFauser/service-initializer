#!/bin/bash
# Docker Compose helper script for local development

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_usage() {
    echo "Usage: $0 <action>"
    echo ""
    echo "Actions:"
    echo "  start       - Start all services"
    echo "  stop        - Stop all services"
    echo "  restart     - Restart all services"
    echo "  reset       - Stop and remove all data (⚠️ deletes volumes)"
    echo "  logs        - Show logs (follow mode)"
    echo "  status      - Show service status"
    echo ""
    echo "Examples:"
    echo "  $0 start"
    echo "  $0 logs"
    echo "  $0 reset"
    exit 1
}

if [ "$#" -lt 1 ]; then
    print_usage
fi

ACTION=$1

cd "$PROJECT_ROOT"

case $ACTION in
    start)
        echo -e "${GREEN}Starting Docker Compose services...${NC}"
        docker-compose up -d
        echo ""
        echo -e "${GREEN}✓ Services started!${NC}"
        echo ""
        echo "Access points:"
        echo "  Kafka UI:   http://localhost:8080"
        echo "  Grafana:    http://localhost:3000 (admin/admin)"
        echo "  Prometheus: http://localhost:9090"
        echo ""
        echo "Run your app with: ./gradlew bootRun"
        ;;

    stop)
        echo -e "${YELLOW}Stopping Docker Compose services...${NC}"
        docker-compose down
        echo -e "${GREEN}✓ Services stopped!${NC}"
        ;;

    restart)
        echo -e "${YELLOW}Restarting Docker Compose services...${NC}"
        docker-compose restart
        echo -e "${GREEN}✓ Services restarted!${NC}"
        ;;

    reset)
        echo -e "${RED}⚠️  WARNING: This will delete all data!${NC}"
        read -p "Are you sure? (yes/no): " -r
        echo
        if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            echo -e "${YELLOW}Stopping and removing all data...${NC}"
            docker-compose down -v
            echo -e "${GREEN}✓ Reset complete!${NC}"
        else
            echo "Cancelled."
        fi
        ;;

    logs)
        echo -e "${GREEN}Showing logs (Ctrl+C to exit)...${NC}"
        docker-compose logs -f
        ;;

    status)
        echo -e "${GREEN}Service Status:${NC}"
        docker-compose ps
        ;;

    *)
        echo -e "${RED}Error: Invalid action '$ACTION'${NC}"
        print_usage
        ;;
esac
