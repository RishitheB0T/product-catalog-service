<!--
BUILD FLOW 01.04 — README.md
First introduced: Phase 01
Purpose: The repository's front page: what works right now, the architecture, how to run it,
         where the evidence is, and the deployment status. It describes reality and labels plans as plans.
Depends on: docs/PROJECT_PLAN.md (scope) and docs/PROGRESS.md (status).
Read by: Anyone opening the repository (GitHub shows it first), including interviewers.
Runtime role: None. Documentation only.
-->

# FORM — The Everyday Edit · Product Catalog Service

A product catalog built from an empty folder in documented phases, with every file explained: a layered
**Spring Boot** REST API over **MySQL**, **Redis** caching for product details, and a **React** storefront that
uses the real API.

> **Status: Phase 01 of 16 complete — planning only. Nothing runs yet.**
> This README always describes what actually works. Anything not built yet is labeled *planned*.

<!-- BUILD FLOW 01.04.01 — What works today -->
## What works today

| Area | Status |
|---|---|
| Plan, architecture, verified version baseline | Done (Phase 01) |
| Spring Boot API: CRUD, search, validation, errors | Planned (Phases 02–07) |
| MySQL schema and measured query optimization | Planned (Phases 04 and 08) |
| Redis product-detail cache | Planned (Phases 09–10) |
| Benchmarks and performance report | Planned (Phase 11) |
| React storefront and local admin | Planned (Phases 12–13) |
| Production containers and CI | Planned (Phase 14) |
| AWS EC2 deployment | Planned (Phase 15; needs AWS access and approval) |

<!-- BUILD FLOW 01.04.02 — Architecture -->
## Architecture (planned)

```text
Browser: React app "FORM"
   │  HTTP  /api/v1/...
   ▼
Vite dev server (development)  ·  Nginx (production; also serves the built React files)
   │  forwards /api/**
   ▼
Spring Boot API
   ProductController   HTTP in and out: parse, validate, choose status codes
   ProductService      business rules, transactions, cache decisions
   ProductRepository   database access through Spring Data JPA
   │                       │
   ▼                       ▼
 Redis                   MySQL 8.4
 short-lived copies      source of truth
 of product details      (schema owned by Flyway migrations)
```

- **Reading one product** checks Redis first. On a miss, it reads MySQL and stores a copy with a short TTL
  (the cache-aside pattern).
- **Searches** always read MySQL, using indexes chosen from measured query plans.
- **Writes** run inside a database transaction. Only after it commits is the cached copy removed.
- The public deployment is **read-only**: the server itself rejects writes.

The numbered runtime steps of each flow are in [`docs/REQUEST_FLOWS.md`](docs/REQUEST_FLOWS.md).

<!-- BUILD FLOW 01.04.03 — Tech stack -->
## Tech stack

Java 21 · Spring Boot 4.1.1 · Spring Data JPA (Hibernate 7) · Bean Validation · Actuator · Flyway 12 ·
MySQL 8.4 LTS · Redis 8.10 · Testcontainers 2 · React 19 · Vite 8 · plain CSS · Docker Compose · Nginx · k6 ·
GitHub Actions. The exact versions, verified against official sources on 2026-10-20, are listed in
[`docs/PROJECT_PLAN.md` §4](docs/PROJECT_PLAN.md#4-stack-and-verified-version-baseline).

<!-- BUILD FLOW 01.04.04 — Repository layout -->
## Repository layout

| Path | Contents | Arrives in |
|---|---|---|
| `backend/` | Spring Boot service | Phase 02 |
| `frontend/` | React + Vite storefront | Phase 12 |
| `infra/` | Compose files, Nginx configuration, EC2 helpers | Phase 03 |
| `scripts/` | PowerShell helpers (Bash only for Linux targets) | Phase 03 |
| `benchmarks/` | Seed tooling, k6 scripts, raw results, query plans | Phase 08 |
| `docs/` | Plan, guides, decisions, evidence | Phase 01 |
| `.github/workflows/` | Continuous integration | Phase 14 |

<!-- BUILD FLOW 01.04.05 — Prerequisites -->
## Prerequisites (Windows 11 development machine)

| Tool | Why it is needed | Notes |
|---|---|---|
| JDK 21 | Compiles and runs the backend | Any JDK 21 distribution works |
| Docker Desktop (WSL 2 engine) | Runs MySQL and Redis locally, builds images, and powers Testcontainers | Must be running |
| Git for Windows | Version control. Its Git Credential Manager handles GitHub sign-in in the browser | — |
| Node.js and npm | Frontend tooling only: Vite dev server, tests, production build | Node 24 LTS or newer |
| Maven | **Not required.** The Maven Wrapper (`mvnw.cmd`) downloads the pinned version | — |
| k6 | Benchmarks | Planned to run from its Docker image, so nothing to install |

<!-- BUILD FLOW 01.04.06 — Running, testing, evidence, deployment -->
## Quick start

Nothing runs yet. Phase 02 adds the first runnable backend.

## Tests

None yet. Tests arrive with the features they protect, starting in Phase 02.

## Performance evidence

No measurements yet. Numbers will appear here only after Phases 08 and 11 measure them, together with the method
and the raw results.

## Deployment status

Not deployed. Phase 15 prepares the EC2 workflow. Deploying needs an AWS account and an explicitly approved choice of
instance, region, and cost.

<!-- BUILD FLOW 01.04.07 — Documentation map -->
## Documentation

| Read this | For |
|---|---|
| [`docs/PROGRESS.md`](docs/PROGRESS.md) | Where the build is right now |
| [`docs/PROJECT_PLAN.md`](docs/PROJECT_PLAN.md) | The plan, the rules, and the 16 phases |
| [`docs/BUILD_GUIDE.md`](docs/BUILD_GUIDE.md) | The project in construction order |
| [`docs/FILE_GUIDE.md`](docs/FILE_GUIDE.md) | Every file and its role |
| [`docs/REQUEST_FLOWS.md`](docs/REQUEST_FLOWS.md) | What happens at runtime, step by step |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Choices and trade-offs |
| [`docs/LEARNING_LOG.md`](docs/LEARNING_LOG.md) | Concepts and exercises for each phase |
