# Local K3s Deployment Guide

One-command local deployment using k3s + Helm. All images are built locally — no external registry needed.

## Prerequisites

```bash
# Docker
# Helm 3
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# k3s
curl -sfL https://get.k3s.io | sh -
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
```

## Deploy

```bash
# One command — builds, loads, installs
./scripts/deploy-local.sh

# With your real Gemini API key
./scripts/deploy-local.sh --api-key YOUR_GEMINI_KEY

# Skip image build (reuse existing images already loaded in k3s)
./scripts/deploy-local.sh --skip-build

# Combine both
./scripts/deploy-local.sh --api-key YOUR_KEY --skip-build
```

**What it does:**
1. Builds 6 Docker images (4 Java services + genai + client)
2. Imports them into the local k3s cluster
3. Installs the Helm chart to namespace `devopss26`

**Help:**
```bash
./scripts/deploy-local.sh --help
```

## Verify

```bash
# Should show 7 pods: postgres + 4 services + genai + client
kubectl get pods -n devopss26
```

Expected:
```text
NAME                              READY   STATUS
postgres-0                        1/1     Running
user-service-xxx                  1/1     Running
checklist-service-xxx             1/1     Running
calendar-service-xxx              1/1     Running
note-service-xxx                  1/1     Running
genai-service-xxx                 1/1     Running
client-xxx                        1/1     Running
```

## Access

| URL | What |
|-----|------|
| `http://localhost` | Frontend (Caddy + React) |
| `http://localhost/api/user/*` | user-service |
| `http://localhost/api/checklist/*` | checklist-service |
| `http://localhost/api/calendar/*` | calendar-service |
| `http://localhost/api/note/*` | note-service |
| `http://localhost/api/genai/*` | genai-service |

## Troubleshoot

| Problem | Fix |
|---------|-----|
| `ImagePullBackOff` | Re-run `./scripts/deploy-local.sh` (images need reloading after k3s restart) |
| `CrashLoopBackOff` on Java services | Check `kubectl logs -n devopss26 -l app=postgres` first — DB must be ready |
| Ingress 404 | Make sure k3s Traefik is running: `kubectl get pods -n kube-system \| grep traefik` |
| Port 80 already in use | k3s Traefik binds port 80. Stop conflicting services (apache/nginx). |

## Teardown

```bash
helm uninstall devopss26 -n devopss26
kubectl delete namespace devopss26
```

## Architecture

```text
┌─────────────┐     ┌──────────────────────────────────────────────┐
│   Browser   │────▶│  Caddy (client pod) :80                      │
└─────────────┘     │  ├── /api/user/*  → user-service:8001        │
                    │  ├── /api/checklist/* → checklist-service:8003│
                    │  ├── /api/calendar/* → calendar-service:8004 │
                    │  ├── /api/note/* → note-service:8005         │
                    │  └── /api/genai/* → genai-service:8006       │
                    └──────────────────────────────────────────────┘
                                         │
                    ┌────────────────────┴────────────────────┐
                    │  PostgreSQL (postgres:17-alpine)        │
                    │  └── Databases: userdb, checklistdb, etc. │
                    └─────────────────────────────────────────┘
```

**Note:** This is the local dev setup. Production uses pre-built images from GHCR — no local build step needed.
