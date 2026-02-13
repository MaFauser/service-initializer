# CI/CD Setup Guide

This guide shows you how to set up automated deployments using GitHub Actions.

## Workflow Overview

```
Developer Workflow:
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  1. Work on features → Push to 'dev' branch            │
│     └─> Auto-deploys to dev cluster                    │
│                                                         │
│  2. Merge dev → main (via PR)                          │
│                                                         │
│  3. Create release with: ./scripts/release.sh v1.0.0   │
│     └─> Auto-deploys to production cluster             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Prerequisites

1. **GitHub CLI** installed locally
   ```bash
   # macOS
   brew install gh

   # Login
   gh auth login
   ```

2. **Kubernetes Clusters** (dev & prod) with:
   - Helm installed
   - `kubectl` access configured

3. **Container Registry**: GitHub Container Registry (ghcr.io) - automatically available

---

## Step 1: Configure GitHub Secrets

Add these secrets to your GitHub repository:

### Go to: Settings → Secrets and variables → Actions → New repository secret

#### Required Secrets:

| Secret Name | Description | How to Get |
|------------|-------------|------------|
| `KUBECONFIG_DEV` | Dev cluster kubeconfig | `cat ~/.kube/config \| base64` |
| `KUBECONFIG_PROD` | Prod cluster kubeconfig | `cat ~/.kube/config-prod \| base64` |
| `DB_PASSWORD` | Production database password | Your secure password |

#### Getting kubeconfig:

```bash
# For dev cluster
kubectl config view --flatten --minify | base64 | pbcopy
# Paste into KUBECONFIG_DEV secret

# For prod cluster (if different)
kubectl config use-context prod-cluster
kubectl config view --flatten --minify | base64 | pbcopy
# Paste into KUBECONFIG_PROD secret
```

---

## Step 2: Enable GitHub Packages

Your Docker images will be stored in GitHub Container Registry (ghcr.io).

1. Go to: Settings → Actions → General
2. Under "Workflow permissions":
   - Select "Read and write permissions"
   - Check "Allow GitHub Actions to create and approve pull requests"
3. Click "Save"

---

## Step 3: Set Up Branch Protection (Optional but Recommended)

Protect your `main` branch to prevent direct pushes:

1. Go to: Settings → Branches → Add rule
2. Branch name pattern: `main`
3. Enable:
   - ✅ Require a pull request before merging
   - ✅ Require status checks to pass before merging
   - ✅ Require conversation resolution before merging
4. Click "Create"

---

## Step 4: Create Dev Branch

```bash
# Create and push dev branch
git checkout -b dev
git push -u origin dev
```

---

## Usage

### Deploy to Dev (Automatic)

```bash
# 1. Make changes
git checkout dev
# ... make changes ...
git add .
git commit -m "Add new feature"

# 2. Push to dev branch
git push origin dev

# ✅ This automatically triggers deployment to dev cluster!
```

### Deploy to Production (Release)

```bash
# 1. Merge dev to main via Pull Request
git checkout main
git pull origin main

# 2. Create release (automatically deploys to prod)
./scripts/release.sh v1.0.0

# ✅ This creates a GitHub release and deploys to production!
```

---

## Workflow Details

### Dev Workflow (`.github/workflows/deploy-dev.yml`)

**Trigger:** Push to `dev` branch

**Steps:**
1. Build Docker image: `ghcr.io/your-repo:dev-{commit-sha}`
2. Push to GitHub Container Registry
3. Deploy to dev cluster using Helm

**Image tags:**
- `dev-{commit-sha}` - Specific commit
- `dev-latest` - Latest dev build

### Production Workflow (`.github/workflows/release.yml`)

**Trigger:** GitHub Release created

**Steps:**
1. Run tests
2. Build Docker image: `ghcr.io/your-repo:v1.0.0`
3. Push to GitHub Container Registry
4. Upload JAR to release
5. Deploy to production cluster using Helm

**Image tags:**
- `v1.0.0` - Version tag
- `latest` - Latest stable release

---

## Monitoring Deployments

### View Workflow Runs
```bash
# List recent workflow runs
gh run list

# View specific run
gh run view

# Watch live run
gh run watch
```

### Check Deployed Version

```bash
# Dev cluster
kubectl get deployment -n development -o jsonpath='{.items[0].spec.template.spec.containers[0].image}'

# Prod cluster
kubectl get deployment -n production -o jsonpath='{.items[0].spec.template.spec.containers[0].image}'
```

---

## Rollback

### Dev Rollback
```bash
# Redeploy previous commit
git revert HEAD
git push origin dev
```

### Production Rollback
```bash
# Option 1: Rollback via Helm
kubectl config use-context prod-cluster
helm rollback prod -n production

# Option 2: Deploy previous version
./scripts/release.sh v1.0.0  # Re-release old version
```

---

## Troubleshooting

### Workflow fails with "permission denied"
- Check that workflow permissions are set to "Read and write"
- Ensure secrets are properly base64 encoded

### Can't push to ghcr.io
- Make sure GitHub Packages is enabled
- Check workflow permissions

### Deployment fails
```bash
# Check workflow logs
gh run view --log-failed

# Check cluster
kubectl get pods -n development
kubectl logs -f deployment/dev-service-chart-app -n development
```

### Image not found
```bash
# Check if image exists
docker pull ghcr.io/your-org/your-repo:v1.0.0

# List all images in GitHub Packages
gh api /user/packages/container/your-repo/versions
```

---

## Local Testing

Test your CI/CD workflow locally using [act](https://github.com/nektos/act):

```bash
# Install act
brew install act

# Test dev workflow
act push -j deploy-dev

# Test release workflow
act release -j release-prod
```

---

## Advanced Configuration

### Custom Docker Registry

To use Docker Hub or another registry instead of ghcr.io:

1. Update `.github/workflows/*.yml`:
   ```yaml
   env:
     REGISTRY: docker.io  # or your registry
     IMAGE_NAME: your-username/service
   ```

2. Add registry credentials:
   ```bash
   # GitHub secrets
   DOCKER_USERNAME
   DOCKER_PASSWORD
   ```

3. Update login step:
   ```yaml
   - name: Log in to Docker Hub
     uses: docker/login-action@v3
     with:
       username: ${{ secrets.DOCKER_USERNAME }}
       password: ${{ secrets.DOCKER_PASSWORD }}
   ```

### Add Slack Notifications

Add to workflow files:
```yaml
- name: Notify Slack
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK }}
    payload: |
      {
        "text": "Deployed ${{ steps.version.outputs.version }} to production"
      }
```

---

## Security Best Practices

✅ **DO:**
- Use GitHub Secrets for sensitive data
- Rotate kubeconfig and passwords regularly
- Use branch protection on `main`
- Review PRs before merging to main
- Use semantic versioning for releases

❌ **DON'T:**
- Commit secrets to git
- Share kubeconfig files
- Skip tests in production workflow
- Deploy directly to prod without testing in dev

---

## Summary

**Development:**
```bash
git checkout dev
# make changes
git push origin dev  # Auto-deploys to dev
```

**Production:**
```bash
git checkout main
git merge dev
./scripts/release.sh v1.0.0  # Auto-deploys to prod
```

That's it! Simple and automated. 🚀
