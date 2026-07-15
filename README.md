# team-devopss26

> An AI-integrated life manager: a set of microservices with a React frontend.

This README covers **setup**, **architecture**, **API references**, **CI/CD & monitoring**, and **student
responsibilities**.

---

## Table of Contents

- [Setup Instructions](#setup-instructions)
- [Architecture](#architecture)
- [API Documentation](#api-documentation)
- [CI/CD and Monitoring](#cicd-and-monitoring)
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
   npm start
   # ≡ docker compose -f infra/docker-compose.yml up --build
   ```

2. **Access the running stack:**

   _Web UIs (open in a browser):_

   | URL                           | What                  |
   |-------------------------------|-----------------------|
   | `http://localhost`            | Frontend (web client) |
   | `http://localhost/prometheus` | Prometheus            |
   | `http://localhost/grafana`    | Grafana               |
   | `http://localhost/swagger`    | Swagger               |

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
┌─────────────┐          ┌───────────────────────────────────────────────────┐
│   Browser   │────────▶│  Caddy (client) :80                               │
└─────────────┘          │  ├── /api/v1/users/*     → user-service:8001      │
                         │  ├── /api/v1/checklists/* → checklist-service:8003│
                         │  ├── /api/v1/events/*     → calendar-service:8004 │
                         │  ├── /api/v1/notes/*      → note-service:8005     │
                         │  └── /api/v1/*            → genai-service:8006    │
                         └───────────────────────────────────────────────────┘
                                                   │
                    ┌──────────────────────────────┴──────────────────────────────┐
                    │  PostgreSQL                                                 │
                    │  └── Databases: user_service_db, checklist_service_db,      │
                    │      calendar_service_db, note_service_db, genai_service_db │
                    └──────────────────────────────┴──────────────────────────────┘
                                                   │
                            ┌──────────────────────┴─────────────────────┐
                            │  Weaviate (vector DB for genai-service)    │
                            └────────────────────────────────────────────┘
```

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

**GenAI Service** — `/api/v1/...`

- `GET /api/v1/health` — health check
- `GET/POST /api/v1/conversations` — list or start conversations
- `GET/DELETE /api/v1/conversations/{conversationId}` — read or delete a conversation
- `POST /api/v1/chat` — send a message and receive an AI response (backed by Weaviate)

For exact request/response schemas, parameters, and error codes, see the OpenAPI specs above — they are the
authoritative contract.

---

## CI/CD and Monitoring

### Continuous Integration — `.github/workflows/ci.yml`

Runs on **every push**. All jobs except the first depend on a successful OpenAPI lint:

1. **openapi-lint** — validates all `api/*.yaml` specs with Redocly.
2. **python-lint** — runs `ruff` over the GenAI service.
3. **python-test** — generates the FastAPI client, then runs `pytest` against a Postgres container.
4. **backend-test** — for each Java service (`user`, `checklist`, `calendar`, `note`): Maven build, tests, and SpotBugs
   static analysis.
5. **frontend-test** — dependency install, Orval codegen, TypeScript typecheck, tests, and production build.

### Continuous Deployment — `.github/workflows/deploy.yml`

Triggered automatically when CI succeeds on `main` (or manually via `workflow_dispatch`):

1. **Detect changed services** via path filters — only affected images are rebuilt. Changes to `services/shared` or
   `api/` cascade to all Java services.
2. **Build & push** changed images to **GHCR** (`ghcr.io/aet-devops26/team-devopss26/*`), tagged `latest` and by commit
   SHA. Skipped services are re-tagged with the SHA so Helm pulls a consistent set.
3. **Deploy to the AET Kubernetes cluster** via Helm (`infra/iac/aet`) into `team-devopss26-prod`, with
   `--rollback-on-failure`.
4. **Verify** the rollout — waits for every deployment/statefulset to be ready and confirms all pods run the expected
   image tag.

Infrastructure-as-code:

- `infra/iac/aet/` — Helm chart for the AET (k8s) cluster
- `infra/iac/azure/` — Terraform + Ansible for Azure (see `infra/iac/azure/README.md`)

### Monitoring

Observability comes from Prometheus and Grafana, included in both the local Docker Compose setup and the Kubernetes
deployment.
Both are deployed in a seperate namespace, namely `team-devopss26-monitoring`.

- **Prometheus** ([`infra/prometheus/prometheus.yml`](infra/prometheus/prometheus.yml)) scrapes every 15s:
    - Java services → `/actuator/prometheus` (ports 8001, 8003, 8004, 8005)
    - GenAI service → `/metrics` (port 8006)
    - UI: `http://localhost:9090`
- **Grafana** is provisioned with the Prometheus datasource ([
  `infra/grafana/provisioning/datasources/prometheus.yml`](infra/grafana/provisioning/datasources/prometheus.yml)); UI:
  `http://localhost:3000`

> **Summary:** CI validates and tests every change → CD builds only what changed and deploys it to the AET cluster via
> Helm with automatic rollback → Prometheus + Grafana provide live metrics and dashboards.

---

## Student Responsibilities

| Student                | High-Level Responsibility | Low-Level Responsibility                                                                                                                                    |
|------------------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Ahmet Coşkun           | GenAI Lead                | - GenAI Service<br/>- Monitoring & Observability<br/>- Project Vision<br/>- Initial OpenAPI Specs<br/>- CI for GenAI                                        |
| Alexander Michael Wudy | Server Owner              | - Java Microservices<br/>- Azure Deployment<br/>- Refined OpenAPI Specs<br/>- CI for Server + CD for Azure<br/>- Docker, docker-compose & Root package.json |
| Werner Richter         | Client Owner              | - React Client<br/>- AET Deployment<br/>- CI for Client + CD for AET<br/>- Ingress Configuration (Caddy - local & deployment)<br/>- Mockups and Design      |