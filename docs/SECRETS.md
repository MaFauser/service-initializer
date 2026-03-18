# Creating Secrets for Dev and Production

This guide explains how to create the Kubernetes Secrets (and optional GitHub Actions secrets) required to run the application when using **dev.yaml** or **prod.yaml**. Local (local.yaml) uses raw credentials in the file and does **not** require Secrets.

---

## When Are Secrets Required?

| Environment | Values file           | Secrets required? |
|-------------|-----------------------|-------------------|
| **Local**   | local.yaml     | **No** – credentials are in the file |
| **Dev**     | dev.yaml       | **Yes** – create before `helm install` |
| **Prod**    | prod.yaml      | **Yes** – create before `helm install` |

Dev and prod **never** store passwords in values files; the chart reads them from Kubernetes Secrets via `existingSecret`.

---

## Required Secrets (Dev and Prod)

The chart expects **two** Secrets per environment. Names and key names must match the values file.

### 1. PostgreSQL (and app datasource) credentials

Used by:
- **PostgreSQL** deployment (database user/password)
- **Application** deployment (`POSTGRES_USER`, `POSTGRES_PASSWORD`)
- **PostgreSQL backup** CronJob (when enabled in prod)

| Env  | Secret name                     | Required keys | Values file reference |
|------|----------------------------------|---------------|------------------------|
| Dev  | `dev-postgresql-credentials`    | `username`, `password` | `postgresql.auth.existingSecret`, `application.datasource.existingSecret` |
| Prod | `prod-postgresql-credentials`   | `username`, `password` | Same |

**Key names** must match `postgresql.auth.usernameKey` and `postgresql.auth.passwordKey` in your values file (defaults in the chart are `username` and `password`).

### 2. Grafana admin credentials

Used by the **Grafana** deployment for the web UI admin user.

| Env  | Secret name                | Required keys      | Values file reference        |
|------|-----------------------------|--------------------|------------------------------|
| Dev  | `dev-grafana-credentials`   | `adminUser`, `adminPassword` | `grafana.auth.existingSecret`, `adminUserKey`, `adminPasswordKey` |
| Prod | `prod-grafana-credentials` | `adminUser`, `adminPassword` | Same |

**Key names** must match `grafana.auth.adminUserKey` and `grafana.auth.adminPasswordKey` (in our env files: `adminUser`, `adminPassword`).

---

## Using a stored kubeconfig (e.g. `.kubeconfig-dev.b64`)

If your kubeconfig is stored as base64 in `.kubeconfig-dev.b64` (as in [docs/ORACLE.md](ORACLE.md)), decode it and set `KUBECONFIG` so `kubectl` and `helm` use the dev cluster. Then create the namespace and Secrets:

```bash
# From the project root
cd /path/to/service-initializer

# Point kubectl at your dev cluster (decode .kubeconfig-dev.b64)
export KUBECONFIG=$(mktemp)
base64 -d < .kubeconfig-dev.b64 > "$KUBECONFIG"

# Create the namespace (required before creating Secrets)
kubectl create namespace development --dry-run=client -o yaml | kubectl apply -f -

# Now create the Secrets (see Development section below)
kubectl create secret generic dev-postgresql-credentials \
  --from-literal=username=service \
  --from-literal=password='YOUR_SECURE_POSTGRES_PASSWORD' \
  -n development
kubectl create secret generic dev-grafana-credentials \
  --from-literal=adminUser=admin \
  --from-literal=adminPassword='YOUR_SECURE_GRAFANA_PASSWORD' \
  -n development
```

Alternatively, use the script that already loads the kubeconfig and runs in a shell:

```bash
./scripts/k8s-dev.sh run
# In the spawned shell, KUBECONFIG is set; then run the kubectl create namespace and create secret commands above.
```

---

## Step-by-Step: Create Secrets Manually

### Development

1. Create the namespace (if it does not exist):

   ```bash
   kubectl create namespace development --dry-run=client -o yaml | kubectl apply -f -
   ```

2. Create the PostgreSQL credentials Secret.  
   Use a strong password; the username should match the DB user your app expects (e.g. `service`):

   ```bash
   kubectl create secret generic dev-postgresql-credentials \
     --from-literal=username=service \
     --from-literal=password='YOUR_SECURE_POSTGRES_PASSWORD' \
     -n development
   ```

3. Create the Grafana credentials Secret:

   ```bash
   kubectl create secret generic dev-grafana-credentials \
     --from-literal=adminUser=admin \
     --from-literal=adminPassword='YOUR_SECURE_GRAFANA_PASSWORD' \
     -n development
   ```

4. Deploy with Helm:

   ```bash
   helm install dev ./infra/helm/stack \
     -f ./infra/helm/stack/config/images.yaml \
   
     -f ./infra/helm/stack/dev.yaml \
     --namespace development \
     --create-namespace
   ```

### Production

1. Create the namespace:

   ```bash
   kubectl create namespace production --dry-run=client -o yaml | kubectl apply -f -
   ```

2. Create the PostgreSQL credentials Secret:

   ```bash
   kubectl create secret generic prod-postgresql-credentials \
     --from-literal=username=service \
     --from-literal=password='YOUR_SECURE_POSTGRES_PASSWORD' \
     -n production
   ```

3. Create the Grafana credentials Secret:

   ```bash
   kubectl create secret generic prod-grafana-credentials \
     --from-literal=adminUser=admin \
     --from-literal=adminPassword='YOUR_SECURE_GRAFANA_PASSWORD' \
     -n production
   ```

4. Deploy with Helm:

   ```bash
   helm install prod ./infra/helm/stack \
     -f ./infra/helm/stack/config/images.yaml \
   
     -f ./infra/helm/stack/prod.yaml \
     --namespace production \
     --create-namespace
   ```

---

## Generating Strong Passwords

Use unique, strong passwords per environment. Examples of generating them:

```bash
# OpenSSL (e.g. 24-character alphanumeric)
openssl rand -base64 24

# Or store in a variable and use when creating the Secret
export POSTGRES_PASSWORD=$(openssl rand -base64 24)
kubectl create secret generic prod-postgresql-credentials \
  --from-literal=username=service \
  --from-literal=password="$POSTGRES_PASSWORD" \
  -n production
```

Keep a secure copy of these passwords (e.g. in a vault or password manager); you will need them to connect to the database or log in to Grafana.

---

## CI/CD: GitHub Actions Secrets

If you use the provided GitHub Actions workflows to deploy, the workflows **create** the Kubernetes Secrets from repository secrets. You only need to configure the repository secrets once; the workflow will create or update the K8s Secrets on each run.

### Repository secrets to add

Go to **Settings → Secrets and variables → Actions → New repository secret**, and add:

| Secret name                 | Used by    | Purpose |
|----------------------------|------------|---------|
| `KUBECONFIG_DEV`           | deploy-dev | Kubeconfig (base64) for the dev cluster |
| `KUBECONFIG_PROD`          | release    | Kubeconfig (base64) for the prod cluster |
| `GHCR_PAT`                 | deploy-dev | GitHub PAT with `read:packages` (image pull) |
| `DEV_POSTGRESQL_PASSWORD`  | deploy-dev | Password for `dev-postgresql-credentials` |
| `DEV_GRAFANA_PASSWORD`     | deploy-dev | Admin password for `dev-grafana-credentials` |
| `DB_PASSWORD`              | release    | Password for `prod-postgresql-credentials` |
| `GRAFANA_PASSWORD`         | release    | Admin password for `prod-grafana-credentials` |

The workflows create the Kubernetes Secrets with fixed usernames (`service` for Postgres, `admin` for Grafana) and the passwords from these repository secrets. You do **not** need to create the K8s Secrets by hand when using CI; just set the repository secrets above.

See [.github/SETUP.md](../.github/SETUP.md) for kubeconfig and first-time setup.

---

## Is PostgreSQL open to the world?

**No.** PostgreSQL is exposed as a **ClusterIP** service: it is only reachable inside the cluster. Nothing on the internet can connect to it directly. With `networkPolicy.enabled: true` (e.g. in prod), only the app and backup job pods are allowed to connect to Postgres.

To connect from your laptop (e.g. DBeaver), you use **port-forwarding**: a tunnel from your machine to the Postgres pod. That does not expose Postgres to the world; only your localhost gets a port.

## Connecting from DBeaver (dev)

1. Start the port-forwards so Postgres is available on localhost (e.g. run the dev script):
   ```bash
   ./scripts/k8s-dev.sh forward
   ```
   This forwards `dev-stack-postgresql:5432` to **localhost:5432**. Leave it running.

2. In DBeaver (or any PostgreSQL client), create a connection:
   - **Host:** `localhost`
   - **Port:** `5432`
   - **Database:** `servicedb`
   - **Username:** same as in your Secret (e.g. `service` for `dev-postgresql-credentials`)
   - **Password:** the password you set when creating `dev-postgresql-credentials`

3. Connect. The traffic goes through the kubectl port-forward tunnel to the cluster; Postgres itself is not exposed publicly.

To forward only Postgres in another terminal:
```bash
export KUBECONFIG=$(mktemp) && base64 -d < .kubeconfig-dev.b64 > "$KUBECONFIG"
kubectl port-forward -n development svc/dev-stack-postgresql 5432:5432
# Then connect DBeaver to localhost:5432
```

---

## Verifying Secrets

After creating the Secrets:

```bash
# List Secrets (names only)
kubectl get secrets -n development
kubectl get secrets -n production

# Check that the expected keys exist (example for dev)
kubectl get secret dev-postgresql-credentials -n development -o jsonpath='{.data}' | jq 'keys'
# Should show: ["password", "username"] (base64-encoded)
```

Do **not** print secret values in logs or docs; use `kubectl get secret ... -o yaml` only when debugging in a secure environment.

---

## Updating a Password

To rotate a password:

1. Create a new Secret with a different name (e.g. `dev-postgresql-credentials-v2`) and the new password, or
2. Update the existing Secret in place (less ideal for rollback):

   ```bash
   kubectl delete secret dev-postgresql-credentials -n development
   kubectl create secret generic dev-postgresql-credentials \
     --from-literal=username=service \
     --from-literal=password='NEW_PASSWORD' \
     -n development
   ```

3. Restart the workloads that use it so they pick up the new Secret:

   ```bash
   kubectl rollout restart deployment/dev-stack-app deployment/dev-stack-postgresql -n development
   ```

If you use a new Secret name, update the values file (`postgresql.auth.existingSecret` and `application.datasource.existingSecret`) and run `helm upgrade`, then delete the old Secret.

**Important:** PostgreSQL reads the Secret only when it **first initializes** the data directory. Restarting the Postgres deployment does **not** change the password stored inside the database. If you recreated or updated the Secret after Postgres was already running, see [Troubleshooting: password authentication failed](#troubleshooting-password-authentication-failed) below.

---

## Troubleshooting: password authentication failed

If the app fails on startup with:

```text
FATAL: password authentication failed for user "service"
(SQL State: 28P01)
```

then the **password in the Kubernetes Secret no longer matches** the password Postgres has for user `service`. This usually happens when:

- The Secret was recreated or updated with a new password **after** Postgres was first deployed, or
- Postgres was deployed earlier (e.g. with a default or different Secret) and the current Secret was created later.

Postgres only reads the Secret when it initializes its data directory; after that, the user password lives inside the DB. So changing the Secret and restarting the app (or even Postgres) does not update the password inside Postgres.

### Fix option 1: Reset the password inside Postgres (keep data)

Make the DB user password match the value currently in the Secret. With the official Postgres image, you can connect as the `postgres` superuser inside the pod (local trust) and set `service`’s password:

1. Get the password currently in the Secret (use the same namespace and secret name as in your values, e.g. dev):

   ```bash
   kubectl get secret dev-postgresql-credentials -n development -o jsonpath='{.data.password}' | base64 -d
   echo
   ```

2. Exec into the Postgres pod and set the `service` user’s password to that value (replace `NEW_PASSWORD` with the output from step 1; escape single quotes in the password or use a here-doc):

   ```bash
   kubectl exec -it -n development deployment/dev-stack-postgresql -- \
     psql -U postgres -d servicedb -c "ALTER USER service PASSWORD 'NEW_PASSWORD';"
   ```

   If your image does not allow local `postgres` connections, use port-forward and connect with whatever admin credentials your image provides.

3. Restart the app so it retries with the updated password:

   ```bash
   kubectl rollout restart deployment/dev-stack-app -n development
   ```

### Fix option 2: Re-initialize Postgres (data loss)

If the dev database can be discarded:

1. Scale down the app (optional, to avoid crash loops): `./scripts/k8s-dev.sh scale-app 0`
2. Delete the Postgres PVC so the next deploy recreates an empty data dir:

   ```bash
   kubectl delete pvc -n development -l app.kubernetes.io/component=postgresql
   # Or the exact PVC name, e.g. dev-stack-postgresql
   ```

3. Delete the Postgres pod so it is recreated and re-initializes using the current Secret:

   ```bash
   kubectl delete pod -n development -l app.kubernetes.io/component=postgresql
   ```

4. Wait for Postgres to be ready, then scale the app back up: `./scripts/k8s-dev.sh scale-app 1`

After either fix, the app should start successfully.

---

## Summary

| Goal                         | Action |
|-----------------------------|--------|
| Run **locally** (no Secrets) | Use `local.yaml`; credentials are in the file. |
| Run **dev** with Secrets    | Create `dev-postgresql-credentials` and `dev-grafana-credentials` in `development`, then `helm install` with `dev.yaml`. |
| Run **prod** with Secrets   | Create `prod-postgresql-credentials` and `prod-grafana-credentials` in `production`, then `helm install` with `prod.yaml`. |
| Deploy via **GitHub Actions** | Set repository secrets `DEV_POSTGRESQL_PASSWORD`, `DEV_GRAFANA_PASSWORD`, `DB_PASSWORD`, `GRAFANA_PASSWORD` (and kubeconfig/PAT); workflows create the K8s Secrets. |

For full deployment steps, see [DEPLOYMENT.md](DEPLOYMENT.md) and [Helm README](../infra/helm/README.md).
