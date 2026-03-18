# CI/CD Setup Guide

This guide shows how to set up the GitHub Actions workflows for this application repo.

## Workflow Overview

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| PR Validation | Pull request to main/dev/testing | Build, test, coverage, push image |
| Push Dev Image | Push to `dev` | Build + push image tagged `dev-latest` |
| Release Image | GitHub Release published | Retag image with version + `latest` |

Deployment to Kubernetes is handled by the [platform-infra](../../platform-infra) repo.

## Prerequisites

1. **GitHub CLI** installed locally
   ```bash
   brew install gh
   gh auth login
   ```

## Configure GitHub Secrets

Go to: **Settings -> Secrets and variables -> Actions -> New repository secret**

| Secret Name | Description |
|-------------|-------------|
| *(none required for basic image push)* | GHCR uses `GITHUB_TOKEN` automatically |

## Enable GitHub Packages

1. Go to: **Settings -> Actions -> General**
2. Under "Workflow permissions":
   - Select "Read and write permissions"
3. Click "Save"

## Usage

### Dev workflow

```bash
git checkout dev
# make changes
git push origin dev    # builds + pushes image tagged dev-latest
```

### Production release

```bash
git checkout main
git merge dev
./scripts/release.sh v1.0.0    # creates GitHub release, triggers image retag
```

## Monitoring

```bash
gh run list
gh run view
gh run watch
```
