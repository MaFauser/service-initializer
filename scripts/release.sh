#!/bin/bash
# Simple release script - creates a GitHub release that triggers production deployment

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_usage() {
    echo "Usage: $0 <version>"
    echo ""
    echo "Creates a GitHub release that automatically deploys to production"
    echo ""
    echo "Examples:"
    echo "  $0 v1.0.0       # Major release"
    echo "  $0 v1.0.1       # Patch release"
    echo "  $0 v1.1.0       # Minor release"
    echo ""
    echo "Version format: vMAJOR.MINOR.PATCH (semantic versioning)"
    exit 1
}

if [ "$#" -ne 1 ]; then
    print_usage
fi

VERSION=$1

# Validate version format
if ! [[ $VERSION =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}Error: Invalid version format${NC}"
    echo "Expected format: vMAJOR.MINOR.PATCH (e.g., v1.0.0)"
    exit 1
fi

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo -e "${RED}Error: GitHub CLI (gh) is not installed${NC}"
    echo "Install from: https://cli.github.com/"
    exit 1
fi

# Check if we're on main branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo -e "${YELLOW}Warning: You're on branch '$CURRENT_BRANCH', not 'main'${NC}"
    read -p "Continue anyway? (yes/no): " -r
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        echo "Cancelled."
        exit 1
    fi
fi

# Check for uncommitted changes
if [[ -n $(git status -s) ]]; then
    echo -e "${RED}Error: You have uncommitted changes${NC}"
    git status -s
    exit 1
fi

# Pull latest changes
echo -e "${GREEN}Pulling latest changes...${NC}"
git pull origin main

# Check if tag already exists
if git rev-parse "$VERSION" >/dev/null 2>&1; then
    echo -e "${RED}Error: Tag $VERSION already exists${NC}"
    exit 1
fi

echo -e "${GREEN}Creating release $VERSION${NC}"
echo ""
echo "This will:"
echo "  1. Create an annotated Git tag"
echo "  2. Push the tag to GitHub"
echo "  3. Create a GitHub release"
echo "  4. Trigger CI/CD to build and deploy to production"
echo ""
read -p "Continue? (yes/no): " -r
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Cancelled."
    exit 1
fi

# Create annotated tag
echo -e "${YELLOW}Creating tag $VERSION...${NC}"
git tag -a "$VERSION" -m "Release $VERSION"

# Push tag
echo -e "${YELLOW}Pushing tag to GitHub...${NC}"
git push origin "$VERSION"

# Create GitHub release (this triggers the workflow)
echo -e "${YELLOW}Creating GitHub release...${NC}"
gh release create "$VERSION" \
    --title "$VERSION" \
    --generate-notes

echo ""
echo -e "${GREEN}✅ Release created successfully!${NC}"
echo ""
echo "The GitHub Actions workflow will now:"
echo "  1. Build Docker image: ghcr.io/$(gh repo view --json nameWithOwner -q .nameWithOwner):$VERSION"
echo "  2. Run tests"
echo "  3. Deploy to production cluster"
echo ""
echo "View the workflow: https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/actions"
echo "View the release: $(gh release view $VERSION --json url -q .url)"
