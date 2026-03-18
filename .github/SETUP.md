# CI/CD Setup Guide

This guide shows how to set up the GitHub Actions workflows for this application repo.

## Workflow Overview

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| PR Validation | Pull request to main/dev/testing | Build, test, coverage, push image |
| Push Dev Image | Push to `dev` | Build + push image tagged `dev-latest` |
| Release Image | GitHub Release published | Retag image with version + `latest` |

Deployment to Kubernetes is handled by the [platform-infra](https://github.com/MaFauser/platform-infra) repo.

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
| `PLATFORM_DEPLOY_TOKEN` | GitHub PAT with `repo` scope for accessing `platform-infra` Helm charts (required for deploy jobs) |
| `KUBECONFIG_DEV` | Base64-encoded kubeconfig for the dev cluster (required for dev deploy) |
| `KUBECONFIG_PROD` | Base64-encoded kubeconfig for the prod cluster (required for prod deploy) |

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

Create a GitHub Release from the [Releases page](../../releases) or use the CLI:

```bash
gh release create v1.0.0 --generate-notes
```

## Monitoring

```bash
gh run list
gh run view
gh run watch
```
