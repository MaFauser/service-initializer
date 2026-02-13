# GitHub Actions Workflows

## Workflows

### 1. Deploy to Dev (`deploy-dev.yml`)
- **Trigger**: Push to `dev` branch
- **Deploys to**: Development cluster
- **Image tag**: `dev-{commit-sha}` and `dev-latest`

### 2. Release to Production (`release.yml`)
- **Trigger**: GitHub Release published
- **Deploys to**: Production cluster
- **Image tag**: `{version}` (e.g., `v1.0.0`) and `latest`

## Quick Start

See [SETUP.md](../SETUP.md) for complete configuration instructions.

### TL;DR

1. **Add secrets** to GitHub repository:
   - `KUBECONFIG_DEV` - Dev cluster access
   - `KUBECONFIG_PROD` - Prod cluster access
   - `DB_PASSWORD` - Production database password

2. **Deploy to dev**:
   ```bash
   git push origin dev
   ```

3. **Deploy to prod**:
   ```bash
   ./scripts/release.sh v1.0.0
   ```
