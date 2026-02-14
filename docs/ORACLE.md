# Deploying to Oracle Kubernetes Engine (OKE)

Step-by-step guide to run locally and deploy via CI to Oracle Cloud OKE.

---

## Overview

| Goal | What you need | When |
|------|---------------|------|
| **Run locally** | OCI CLI + kubeconfig with OCI exec | One-time setup, then `./scripts/k8s-dev.sh` |
| **Deploy via CI** | ServiceAccount token kubeconfig in `KUBECONFIG_DEV` | One-time setup, then push to `dev` |

---

## Prerequisites

- [ ] Oracle Cloud account with an OKE cluster
- [ ] `kubectl` installed (`brew install kubectl`)
- [ ] `helm` installed (`brew install helm`)

---

## Part 1: Local Access (run `./scripts/k8s-dev.sh`)

### Step 1.1: Install OCI CLI

```bash
brew install oci-cli
```

### Step 1.2: Configure OCI CLI

```bash
oci setup config
```

You will be prompted for:

| Prompt | Where to find it |
|--------|------------------|
| User OCID | OCI Console → Profile (top-right) → User Settings → OCID |
| Tenancy OCID | OCI Console → Profile → Tenancy |
| Region | e.g. `sa-saopaulo-1` |
| Generate new API key? | `Y` |
| Directory for keys | Press Enter (default `~/.oci`) |

**Add the public key to OCI:**

1. OCI Console → **Profile** → **User Settings** → **API Keys** → **Add API Key**
2. Choose **Paste public key**
3. Paste contents of `~/.oci/oci_api_key_public.pem` (or the path shown by `oci setup config`)

### Step 1.3: Create kubeconfig (public endpoint)

Use your cluster OCID (from OCI Console → Kubernetes Clusters → your cluster → Cluster information).

```bash
oci ce cluster create-kubeconfig \
  --cluster-id <your-cluster-ocid> \
  --file /tmp/config-oke \
  --region sa-saopaulo-1 \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT
```

### Step 1.4: Save kubeconfig for local use

```bash
cd /path/to/service-initializer
base64 -i /tmp/config-oke | tr -d '\n' > .kubeconfig-dev.b64
```

The file `.kubeconfig-dev.b64` is in `.gitignore` and will not be committed.

### Step 1.5: Verify local access

```bash
./scripts/k8s-dev.sh pods
```

You should see pods (or "No resources found" if nothing is deployed yet).

**Other commands:**

| Command | Description |
|---------|-------------|
| `./scripts/k8s-dev.sh pods` | List pods |
| `./scripts/k8s-dev.sh forward` | Port-forward app, Grafana, Kafka UI |
| `./scripts/k8s-dev.sh logs` | Tail app logs |
| `./scripts/k8s-dev.sh status` | Helm status |

---

## Part 2: CI/CD Deployment (GitHub Actions)

The OCI exec kubeconfig works locally but **not in CI** (no OCI CLI, no interactive auth). For GitHub Actions you need a **ServiceAccount token** kubeconfig.

### Step 2.1: Create ServiceAccount and CI kubeconfig

**Prerequisite:** Local access must work (Part 1 complete).

```bash
./scripts/setup-oke-ci-kubeconfig.sh
```

This script:

1. Creates ServiceAccount `kubeconfig-sa` in `kube-system`
2. Binds it to `cluster-admin`
3. Creates a token secret
4. Builds a kubeconfig with the token (no OCI exec)
5. Outputs the base64-encoded kubeconfig

### Step 2.2: Add secret to GitHub

1. Copy the **entire base64 output** from the script
2. GitHub → **Settings** → **Secrets and variables** → **Actions**
3. **New repository secret** (or edit existing)
   - Name: `KUBECONFIG_DEV`
   - Value: paste the base64 string
4. Save

### Step 2.3: Deploy

Push to the `dev` branch:

```bash
git push origin dev
```

The workflow will build, push the image, and deploy to OKE.

---

## Part 3: Private Endpoint (Optional – Bastion)

If your cluster has **only** a private API endpoint (no public):

1. OCI Console → **Bastions** → **Create bastion** (in same VCN as cluster)
2. **Create session** → **SSH port forwarding session**
   - Target IP: Kubernetes API endpoint (e.g. `10.0.0.5`)
   - Target port: `6443`
   - Add your SSH public key
3. Run the SSH tunnel command (from session details) **before** using `k8s-dev.sh`
4. Edit your kubeconfig `server:` to `https://127.0.0.1:<local-port>` and match the tunnel port

---

## Summary

| Task | Command / Action |
|------|------------------|
| Local access (first time) | OCI CLI setup → create kubeconfig → save to `.kubeconfig-dev.b64` |
| Local access (daily) | `./scripts/k8s-dev.sh pods` (or `forward`, `logs`, `status`) |
| CI setup (first time) | `./scripts/setup-oke-ci-kubeconfig.sh` → add output to `KUBECONFIG_DEV` |
| Deploy to dev | `git push origin dev` |
| Deploy to prod | Create a release (see [.github/SETUP.md](../.github/SETUP.md)) |

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| `executable oci not found` | Install OCI CLI: `brew install oci-cli` |
| `Could not find config file at ~/.oci/config` | Run `oci setup config` |
| `server asked for credentials` | Check IAM: user needs `use cluster-family` in compartment |
| `TLS handshake timeout` | Use public endpoint or ensure bastion tunnel is running |
| `Invalid value for --token-version` | Use `2.0.0` not `2.0` |
| CI: `couldn't get version/kind` | Use ServiceAccount kubeconfig (Part 2), not OCI exec kubeconfig |
