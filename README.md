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
- [AI / LLM Configuration](#ai--llm-configuration)
  - [Supported Models](#supported-models)
  - [Environment Variables](#environment-variables)
  - [Defaults by Deployment](#defaults-by-deployment)
- [Mock Data](#mock-data)
  - [Seeded Mock User](#seeded-mock-user)
  - [Seeded Mock Data](#seeded-mock-data)
  - [Resetting the Seed](#resetting-the-seed)
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
- `POST /api/v1/chat` — send a message and receive an AI response (backed by Weaviate); supports model selection (`local`, `gemini`, `gemini-lite`, `groq-llama`, `mistral`, `cohere`)

For exact request/response schemas, parameters, and error codes, see the OpenAPI specs above — they are the
authoritative contract.

---

## AI / LLM Configuration

The `genai-service` is the only model-aware component. It wraps LangChain chains and dispatches each chat request to
one of several supported LLM backends. The active backend is chosen at **request time** (via the `model` field on
`POST /api/v1/chat`) and can be overridden globally with the `DEFAULT_LLM_MODEL` environment variable.

### Supported Models

| Model key         | Backend                                 | Requires                                   | Notes                                                                               |
|-------------------|-----------------------------------------|--------------------------------------------|-------------------------------------------------------------------------------------|
| `local`           | Self-hosted Ollama (e.g. `llama3.1`)    | Running Ollama container (`local` profile) | Offline / no API key. Default for local Docker Compose dev. Slow on CPU (~6 tok/s). |
| `gemini`          | Google Gemini (`gemini-3.1-flash-lite`) | `GEMINI_API_KEY`                           | Default for k8s / Azure. Cheapest hosted option, fast.                              |
| `gemini-lite`     | Alias for `gemini`                      | `GEMINI_API_KEY`                           | Same backend, kept as an explicit name for client-side labelling.                   |
| `groq-llama`      | Groq (`llama-3.1-8b-instant`)           | `GROQ_API_KEY`                             | Hosted Llama via Groq; very fast inference.                                         |
| `mistral`         | Mistral (`mistral-small-latest`)        | `MISTRAL_API_KEY`                          | Hosted Mistral model.                                                               |
| `cohere`          | Cohere (`command-r`)                    | `COHERE_API_KEY`                           | Hosted Cohere model.                                                                |
| _(anything else)_ | Falls back to Gemini                    | `GEMINI_API_KEY`                           | Unknown / unset `model` values default to the hosted Gemini backend.                |

The resolved model identifier is reported back in the chat response (`model` field) so the web client can display
which LLM actually answered (e.g. `llama3.1` for the local Ollama deployment instead of the hardcoded `gemini` label).

### Environment Variables

| Variable            | Purpose                                                                                                            | Used by         |
|---------------------|--------------------------------------------------------------------------------------------------------------------|-----------------|
| `DEFAULT_LLM_MODEL` | Backend used when a request omits `model`.                                                                         | `genai-service` |
| `OLLAMA_BASE_URL`   | Ollama HTTP endpoint. Inside Docker Compose, must be `http://ollama:11434` (not `localhost`).                      | `genai-service` |
| `LOCAL_LLM_MODEL`   | Concrete Ollama model name (e.g. `llama3.1`, `llama3.2:1b`, `phi3:mini`). Pulled on first start.                   | `genai-service` |
| `GEMINI_API_KEY`    | Google Gemini API key. Empty in local `.env`, supplied via Secret in k8s.                                          | `genai-service` |
| `GROQ_API_KEY`      | Groq API key.                                                                                                      | `genai-service` |
| `MISTRAL_API_KEY`   | Mistral API key.                                                                                                   | `genai-service` |
| `COHERE_API_KEY`    | Cohere API key.                                                                                                    | `genai-service` |
| `LLM_TIMEZONE`      | IANA zone name used to compute the temporal context injected into the LLM system prompt. Default: `Europe/Berlin`. | `genai-service` |

In local development, all of the above live in `infra/.env` and are loaded by every service via the `app-template`'s
`env_file`. Empty values (e.g. `GEMINI_API_KEY=""`) are intentional — they let the local stack start without paid
API access by defaulting to `local` (Ollama).

In the AET deployment, the same variables are set in:

- `infra/iac/aet/templates/genai-service/configmap.yaml` (non-secret values, e.g. `DEFAULT_LLM_MODEL=gemini`,
  service URLs)
- `infra/iac/aet/templates/genai-service/secret.yaml` (API keys)
- The k8s chart deliberately does **not** provision an Ollama container, so the `local` model is only available
  locally.

### Defaults by Deployment

| Deployment             | `DEFAULT_LLM_MODEL` | Reasoning                                                                                                                          |
|------------------------|---------------------|------------------------------------------------------------------------------------------------------------------------------------|
| Local (Docker Compose) | `local` (Ollama)    | Self-contained, no API keys, works offline. Override by setting `DEFAULT_LLM_MODEL=gemini` (and `GEMINI_API_KEY`) in `infra/.env`. |
| Azure (Docker Compose) | `gemini`            | Also uses Docker containers, but no Ollama container is provisioned here, so fallback is used.                                     |
| k8s / AET (Azure)      | `gemini`            | No Ollama container is provisioned in the cluster, so the hosted fallback is used.                                                 |

To switch the local stack from Ollama to Gemini for a session:

```bash
# In infra/.env:
DEFAULT_LLM_MODEL=gemini
GEMINI_API_KEY=<your-key>

docker compose -f infra/docker-compose.yml --profile local up --build -d genai-service-app
```

To use a smaller local model (faster cold start, less RAM) on hardware that struggles with `llama3.1`:

```bash
# In infra/.env:
LOCAL_LLM_MODEL=llama3.2:1b    # or phi3:mini, qwen2.5:3b, etc.
docker compose -f infra/docker-compose.yml --profile local up --build -d genai-service-app
# Trigger a fresh pull:
docker exec ollama ollama pull llama3.2:1b
```

---

## Mock Data

For local development and demos, the databases are pre-seeded with a mock user and a small set of related data. The seed
runs automatically via Liquibase on every fresh database migration.

### Seeded Mock User

| Field    | Value                                                                          |
|----------|--------------------------------------------------------------------------------|
| Username | `mock`                                                                         |
| Password | `password`                                                                     |
| User ID  | `-1` (negative on purpose, so it cannot collide with auto-assigned real users) |

The password is stored as a BCrypt-12 hash. The user is created by
`services/user-service/src/main/resources/db/changelog/db.changelog-v2-insert-mock-user.xml`.

### Seeded Mock Data

All seed data references `user_id = -1`, so it is owned by the mock user. The mock data lives in per-service `v2` changelogs:

| Service              | Changelog                                                                                          | Seeded records                                  |
|----------------------|----------------------------------------------------------------------------------------------------|-------------------------------------------------|
| **user-service**     | `services/user-service/.../db.changelog-v2-insert-mock-user.xml`                                  | 1 user (`mock`)                                 |
| **note-service**     | `services/note-service/.../db.changelog-v2-insert-mock-notes.xml`                                  | 4 notes                                         |
| **checklist-service**| `services/checklist-service/.../db.changelog-v2-insert-mock-checklists.xml`                        | 3 checklists with 12 items (mix of done/open)  |
| **calendar-service** | `services/calendar-service/.../db.changelog-v2-insert-mock-calendar-events.xml`                    | 5 events (standup, lecture, sprint review, etc.) |

After `npm run start`, log in with `mock` / `password` and the frontend will show the seeded notes, checklists, and calendar
events.

### Resetting the Seed

The seed lives in the persistent `services-db` Docker volume, so re-running `docker compose up` will not re-apply it
once Liquibase has recorded the changesets. To start from a clean seeded state, remove the volume and restart:

```bash
docker compose -f infra/docker-compose.yml down -v
npm run start
```

> **Note:** The mock user's ID is `-1` to keep it out of the auto-increment sequence. Real users registered through
> `POST /api/v1/users/auth/register` will receive positive IDs (`1`, `2`, …) and never see the mock data.

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