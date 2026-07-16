# team-devopss26

> An AI-integrated life manager: a set of microservices with a React frontend.

This README covers **setup**, **architecture**, **API references**, **CI/CD & monitoring**, and **student
responsibilities**.

---

## Table of Contents

- [Setup Instructions](#setup-instructions)
  - [Prerequisites](#prerequisites)
  - [Local Development (Docker Compose)](#local-development-docker-compose)
  - [OpenAPI Code Generation](#openapi-code-generation)
  - [Tests](#tests)
- [Architecture](#architecture)
  - [Components](#components)
  - [High-Level Architecture](#high-level-architecture)
  - [Design Notes](#design-notes)
- [API Documentation](#api-documentation)
  - [Viewing the API](#viewing-the-api)
  - [Endpoint Overview](#endpoint-overview)
- [CI/CD and Monitoring](#cicd-and-monitoring)
  - [Live Deployments](#live-deployments)
  - [Continuous Integration](#continuous-integration--githubworkflowsciyml)
  - [Continuous Deployment (AET)](#continuous-deployment--githubworkflowsdeployyml)
  - [Azure Deployment](#azure-deployment--githubworkflowsdeploy-azureyml)
  - [Azure VM Start](#azure-vm-start--githubworkflowsstart-vmyml)
  - [Monitoring](#monitoring)
- [Student Responsibilities](#student-responsibilities)

---

## Setup Instructions

### Prerequisites

| Tool                            | Why                                |
|---------------------------------|------------------------------------|
| **Docker** + **Docker Compose** | Run the full stack locally         |
| **Java 21** (Corretto)          | Build the backend services         |
| **Node.js 22**                  | Frontend + OpenAPI code generation |
| **Python 3.11**                 | GenAI service                      |

### Local Development (Docker Compose)

1. **Start everything** — builds and runs the 4 Java services, the GenAI service, the web client, PostgreSQL, Weaviate,
   Prometheus, and Grafana:
   ```bash
   npm run start
   # ≡ docker compose -f infra/docker-compose.yml up --build
   ```

2. **Access the running stack:**

   _Web UIs (open in a browser):_

   | URL                                 | What                  |
   |-------------------------------------|-----------------------|
   | `http://localhost`                  | Frontend (web client) |
   | `http://localhost/prometheus/query` | Prometheus            |
   | `http://localhost/grafana`          | Grafana               |
   | `http://localhost/swagger`          | Swagger               |

   _Backend APIs (JSON, not browser UIs — use the frontend, the specs, or curl/Postman):_

   | Port   | Service           | API base path            |
   |--------|-------------------|--------------------------|
   | `8001` | user-service      | `/api/v1/users/...`      |
   | `8003` | checklist-service | `/api/v1/checklists/...` |
   | `8004` | calendar-service  | `/api/v1/events/...`     |
   | `8005` | note-service      | `/api/v1/notes/...`      |
   | `8006` | genai-service     | `/api/v1/...`            |

### OpenAPI Code Generation

Clients are generated from the specs in `api/`. After editing a spec, regenerate:

```bash
npm run openapi        # clean + generate all clients (backend + frontend)
npm run lint:openapi   # validate all OpenAPI specs with Redocly
```

Generated clients live under each service's `generated/` folder and `web-client/src/openapi.ts`. **Do not edit generated
files manually.**

### Tests

```bash
npm test                # lint OpenAPI + backend + frontend
npm run test:backend    # build/test Java services
npm run test:frontend   # typecheck/test/build web client
```

---

## Architecture

The system is a set of independent microservices — each owning its own data — plus a single-page frontend and shared
infrastructure.

### Components

| Component             | Tech                                 | Port         | Responsibility                                           |
|-----------------------|--------------------------------------|--------------|----------------------------------------------------------|
| **web-client**        | React + Vite (TanStack Router/Query) | 80           | User-facing SPA; talks to services via `/api/*`          |
| **user-service**      | Java 21 / Spring Boot                | 8001         | User registration, login, token/auth, public key         |
| **checklist-service** | Java 21 / Spring Boot                | 8003         | To-do lists and checklist items                          |
| **calendar-service**  | Java 21 / Spring Boot                | 8004         | Calendar events                                          |
| **note-service**      | Java 21 / Spring Boot                | 8005         | Note management                                          |
| **genai-service**     | Python 3.11 / FastAPI                | 8006         | AI chat & conversations; uses Weaviate for vector search |
| **PostgreSQL**        | Postgres 12.8                        | 5432         | Persistent storage; one database per service             |
| **Weaviate**          | Weaviate 1.27                        | 8080 / 50051 | Vector database for the GenAI service                    |
| **Prometheus**        | Prometheus                           | 9090         | Metrics scraping & storage                               |
| **Grafana**           | Grafana                              | 3000         | Metrics dashboards                                       |

### High-Level Architecture

```text
┌─────────────┐           ┌───────────────────────────────────────────────────┐
│   Browser   │────────▶ │  Client :80 (serves React SPA)                    │
└─────────────┘           │  └── Proxies /api/* to backend services:          │
                          │  ├── /api/v1/users/*     → user-service:8001      │
                          │  ├── /api/v1/checklists/* → checklist-service:8003│
                          │  ├── /api/v1/events/*     → calendar-service:8004 │
                          │  ├── /api/v1/notes/*      → note-service:8005     │
                          │  └── /api/v1/*            → genai-service:8006    │
                          └───────────────────────────────────────────────────┘
                                                    │
                     ┌──────────────────────────────┴──────────────────────────────┐
                     │  PostgreSQL (single container, multiple databases)          │
                     │  └── user_service_db, checklist_service_db,                 │
                     │      calendar_service_db, note_service_db, genai_service_db │
                     └──────────────────────────────┴──────────────────────────────┘
                                                    │
                             ┌──────────────────────┴─────────────────────┐
                             │  Weaviate (vector DB for genai-service)    │
                             └────────────────────────────────────────────┘
```

> **Note:** In the AET Kubernetes deployment, a Caddy-based ingress handles routing instead of the local direct proxy.

### Design Notes

- Each Java service uses a variant of the **hexagonal architecture** and shares code via the `services/shared` Maven
  module.
- Frontend and backend clients are **generated from the OpenAPI specs** — the specs are the single source of truth for
  the API contract.
- Locally, services run via Docker Compose; in production they run as containers in a Kubernetes (k8s/AET) cluster
  behind an ingress.

---

## API Documentation

The API is described by OpenAPI 3.0 specs. The combined entrypoint is [`api/openapi.yaml`](api/openapi.yaml), which
links to the per-service specs:

| Service            | Spec                                                       |
|--------------------|------------------------------------------------------------|
| Combined           | [`api/openapi.yaml`](api/openapi.yaml)                     |
| User Service       | [`api/user-service.yaml`](api/user-service.yaml)           |
| Checklist Service  | [`api/checklist-service.yaml`](api/checklist-service.yaml) |
| Calendar Service   | [`api/calendar-service.yaml`](api/calendar-service.yaml)   |
| Note Service       | [`api/note-service.yaml`](api/note-service.yaml)           |
| GenAI Service      | [`api/genai-service.yaml`](api/genai-service.yaml)         |
| Shared definitions | [`api/common.yaml`](api/common.yaml)                       |

### Viewing the API

- **Rendered docs:** There is a hosted Swagger UI by default. It can be accessed via the `/swagger` path.
- **Live endpoints:** Once running, services are reachable at the ports in [Architecture](#architecture), e.g.
  `http://localhost:8001/api/v1/users/auth/register`.

### Endpoint Overview

> A brief textual explanation of the endpoints, in addition to the OpenAPI specs.

**User Service** — `/api/v1/users/...`

- `POST /api/v1/users/auth/register` — register a new user
- `POST /api/v1/users/auth/login` — authenticate and receive a token
- `POST /api/v1/users/auth/check-token` — validate an existing token
- `GET  /api/v1/users/auth/public-key` — retrieve the public key for token verification

**Checklist Service** — `/api/v1/checklists/...`

- `GET/POST /api/v1/checklists` — list or create checklists
- `GET/PUT/DELETE /api/v1/checklists/{id}` — read, update, or delete a checklist
- `GET/POST /api/v1/checklists/{id}/items` — list or add items
- `PUT/DELETE /api/v1/checklists/{id}/items/{itemId}` — update or remove an item

**Calendar Service** — `/api/v1/events/...`

- `GET/POST /api/v1/events` — list or create events
- `GET/PUT/DELETE /api/v1/events/{id}` — read, update, or delete an event

**Note Service** — `/api/v1/notes/...`

- `GET/POST /api/v1/notes` — list or create notes
- `GET/PUT/DELETE /api/v1/notes/{id}` — read, update, or delete a note

**GenAI Service** — `/api/v1/...` (all endpoints except health require JWT authentication)

- `GET /api/v1/health` — health check
- `POST /api/v1/conversations` — create a new chat conversation
- `GET /api/v1/conversations/{conversationId}` — get a conversation with all messages
- `DELETE /api/v1/conversations/{conversationId}` — delete a conversation
- `POST /api/v1/chat` — send a message and receive an AI response (backed by Weaviate); supports model selection (`gemini`, `gemini-lite`, `groq-llama`, `mistral`, `cohere`)

For exact request/response schemas, parameters, and error codes, see the OpenAPI specs above — they are the
authoritative contract.

---

## CI/CD and Monitoring

### Live Deployments

| Environment | URL                                           | Description                      |
|-------------|-----------------------------------------------|----------------------------------|
| **Azure**   | http://20.91.193.39/                          | Production deployment on Azure   |
| **AET**     | https://devopss26.student.k8s.aet.cit.tum.de/ | Production deployment on AET k8s |

---

### Continuous Integration — `.github/workflows/ci.yml`

Runs on **every push** to any branch (except changes to `infra/monitoring/**`, `infra/prometheus/**`, `infra/grafana/**`). All jobs (except `openapi-lint`) depend on a successful lint result:

| Job               | Dependencies   | What it does                                                                                                                |
|-------------------|----------------|-----------------------------------------------------------------------------------------------------------------------------|
| **openapi-lint**  | — (runs first) | Validates all `api/*.yaml` specs with Redocly. Blocks all other jobs on failure. Changes to monitoring configs are ignored. |
| **python-lint**   | openapi-lint   | Runs `ruff` linter over the GenAI service (`services/genai-service`).                                                       |
| **python-test**   | openapi-lint   | Generates FastAPI client from spec, spins up a Postgres container, runs `pytest`.                                           |
| **backend-test**  | openapi-lint   | For each Java service (`user`, `checklist`, `calendar`, `note`): `mvn clean verify` + SpotBugs static analysis.             |
| **frontend-test** | openapi-lint   | Installs deps, runs Orval codegen, TypeScript typecheck, tests, and production build.                                       |

### Continuous Deployment — `.github/workflows/deploy.yml`

Triggered automatically when CI succeeds on `main`, or manually via `workflow_dispatch`. Targets the **AET Kubernetes cluster**.

| Job                | Description                                                                                                                                                                                                                                                                                                      |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **detect**         | Uses `dorny/paths-filter` to detect which services changed. Changes to `services/shared` or `api/` cascade to all Java services. Outputs a matrix of services to build and a list of images to skip-tag.                                                                                                         |
| **build-and-push** | For each changed service: builds the Docker image and pushes to **GHCR** (`ghcr.io/aet-devops26/team-devopss26/<service>`) with two tags: `latest` and the commit SHA.                                                                                                                                           |
| **tag-skipped**    | Re-tags any skipped (unchanged) images from `latest` → SHA so Helm deploys a consistent set of images.                                                                                                                                                                                                           |
| **deploy-to-k8s**  | Configures `kubectl` (v4) and `helm` (v4.2.3), pre-creates Kubernetes secrets (postgres-secret, per-service secrets with DB credentials), then runs `helm upgrade --install` with `--wait --timeout 15m --history-max 10`. Includes a `verify deployment` step that checks all pods and deployments are healthy. |

> **Note:** The Azure deployment is managed separately via Terraform + Ansible (`infra/iac/azure/`). See `infra/iac/azure/README.md` for details.

### Azure Deployment — `.github/workflows/deploy-azure.yml`

Manual trigger only (`workflow_dispatch`). Deploys to the Azure VM using Terraform + Ansible:

| Step                          | Description                                                                                                    |
|-------------------------------|----------------------------------------------------------------------------------------------------------------|
| **Terraform Init/Plan/Apply** | Initializes and applies the Terraform configuration in `infra/iac/azure/` to provision/update Azure resources. |
| **Start Azure VM**            | Starts the `devops-vm` VM in the `devops-rg` resource group via `az vm start`.                                 |
| **Ansible Playbook**          | Connects to the VM via SSH and runs `playbook.yml` to configure and deploy the application.                    |

### Azure VM Start — `.github/workflows/start-vm.yml`

Manual trigger only (`workflow_dispatch`). Starts the Azure VM and waits for it to be SSH-reachable, then starts the Docker Compose stack on the VM. Useful for pre-warming the VM or recovering from a stopped state.

### Monitoring

Observability comes from Prometheus and Grafana, included in both the local Docker Compose setup and the AET and Azure deployments.

- **Local & Azure:** Both Prometheus and Grafana run as Docker containers in `infra/docker-compose.yml` alongside the application services. Prometheus scrapes every 15s:
    - Java services → `/actuator/prometheus` (ports 8001, 8003, 8004, 8005)
    - GenAI service → `/metrics` (port 8006)
    - UI: `http://localhost:9090` (Prometheus) / `http://localhost:3000` (Grafana)
- **AET Kubernetes:** Prometheus and Grafana are deployed via separate k8s manifests in `infra/monitoring/` into the `team-devopss26-monitoring` namespace (managed independently from the Helm app deployment).
- **Grafana dashboards** for GenAI and microservices are provisioned at [`infra/grafana/dashboards/`](infra/grafana/dashboards/).

> **Summary:** CI validates and tests every change → CD builds only what changed and deploys it to the AET cluster via
> Helm (`--wait --timeout 15m`) → Prometheus + Grafana provide live metrics and dashboards.

---

## Student Responsibilities

| Student                | High-Level Responsibility | Low-Level Responsibility                                                                                                                                    |
|------------------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Ahmet Coşkun           | GenAI Lead                | - GenAI Service<br/>- Monitoring & Observability<br/>- Project Vision<br/>- Initial OpenAPI Specs<br/>- CI for GenAI                                        |
| Alexander Michael Wudy | Server Owner              | - Java Microservices<br/>- Azure Deployment<br/>- Refined OpenAPI Specs<br/>- CI for Server + CD for Azure<br/>- Docker, docker-compose & Root package.json |
| Werner Richter         | Client Owner              | - React Client<br/>- AET Deployment<br/>- CI for Client + CD for AET<br/>- Ingress Configuration (Caddy)<br/>- Mockups and Design                           |