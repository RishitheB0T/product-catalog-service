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

> **Status: Phase 04 of 16 complete.** The backend starts against MySQL, Flyway creates the product schema, and
> the dev profile loads a 24-product demo catalog. There is no HTTP API for products yet (Phase 05). This README
> always describes what actually works; anything not built yet is labeled *planned*.

<!-- BUILD FLOW 01.04.01 — What works today -->
## What works today

| Area | Status |
|---|---|
| Plan, architecture, verified version baseline | Done (Phase 01) |
| Spring Boot skeleton: build, tests, profiles, health check | Done (Phase 02) |
| Local MySQL 8.4 and Redis 8.10 in Docker Compose, with a one-command helper | Done (Phase 03) |
| Product schema (Flyway), JPA entity and repository, dev demo catalog, tests on real MySQL | Done (Phase 04) |
| Spring Boot API: CRUD, search, validation, errors | Planned (Phases 05–07) |
| Measured query optimization | Planned (Phase 08) |
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

<!-- BUILD FLOW 01.04.08 — Ports (added in Phase 03) -->
## Ports

The development machine runs several projects, so this one never binds framework defaults (8080, 3306, 6379,
5173, 80) on the host. It uses its own **24xxx** block: `24` followed by the last three digits of the usual port.
Local services listen on `127.0.0.1` only, so other devices on the network cannot reach them.

| Service | Host address | Inside its container | Since |
|---|---|---|---|
| Backend, `dev` profile | `127.0.0.1:24081` | — (runs directly on Windows) | Phase 03 |
| Backend jar, manual production-style run | `127.0.0.1:24082` (passed on the command line) | — | Phase 03 |
| MySQL 8.4 | `127.0.0.1:24306` | 3306 | Phase 03 |
| Redis 8.10 | `127.0.0.1:24379` | 6379 | Phase 03 |
| Vite dev server | 24173 (planned) | — | Phase 12 |
| Production-style stack through Nginx | 24080 (planned) | 80 | Phase 14 |

<!-- BUILD FLOW 01.04.06 — Running, testing, evidence, deployment -->
## Quick start

Run these in PowerShell from the repository root. You need JDK 21 and Docker Desktop (running). The first runs
download Maven, the Java libraries, and the two container images.

**1. Start MySQL and Redis.** The first `up` creates `infra/.env` with new random passwords (git-ignored) and waits
until both containers report healthy.

```powershell
.\scripts\dev-services.ps1 up
```

Other actions: `status`, `logs`, `down` (stops and removes the containers; the database volume is kept), and
`reset -Force` (also deletes the database volume).

**2. Run the backend** with the dev profile:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**3. Check it** from a second terminal:

```powershell
curl.exe http://localhost:24081/actuator/health
```

You should see `"status":"UP"` together with component details, including `db` (MySQL), which only the dev profile
shows. On the first start Flyway creates the product table, and the dev profile loads 24 demo products (log line
`Loaded 24 demo products`). Stop the app with Ctrl+C. Redis is used from Phase 09.

- Keep the quotes around `-Dspring-boot.run.profiles=dev`. Windows PowerShell splits an unquoted argument like
  this at its first dot, so Maven would receive the wrong setting.
- The backend reads the database password from `infra/.env`, so step 1 must have run at least once.

**4. Look at the data** (the SQL goes in through standard input, which avoids Windows PowerShell's quoting problems):

```powershell
"SELECT sku, name, category, price, stock_quantity FROM product LIMIT 5;" | docker compose --env-file infra\.env -f infra\compose.dev.yaml exec -T mysql sh -c 'MYSQL_PWD=$MYSQL_PASSWORD mysql -u $MYSQL_USER $MYSQL_DATABASE --table'
```

**Production-style run of the packaged jar.** The `prod` profile contains no database settings on purpose: a server
supplies them as environment variables, which Spring Boot maps onto its properties (`SPRING_DATASOURCE_URL` becomes
`spring.datasource.url`). Here they point at the local MySQL container:

```powershell
cd backend
.\mvnw.cmd clean verify
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:24306/catalog?connectionTimeZone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "catalog"
$env:SPRING_DATASOURCE_PASSWORD = (Select-String -LiteralPath ..\infra\.env -Pattern '^MYSQL_PASSWORD=(.*)$').Matches[0].Groups[1].Value
java -jar target\catalog-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --server.port=24082 --server.address=127.0.0.1
```

Expected: health without component details, no banner, and no demo data. The `$env:` variables last until you close
that terminal. Without the last two options the jar would listen on Spring Boot's default, port 8080 on every network
interface, which suits a container but not a shared development machine.

## Tests

Docker Desktop must be running: the tests start their own MySQL 8.4.11 container through Testcontainers, separate
from the development database.

```powershell
cd backend
.\mvnw.cmd verify
```

21 tests in three classes:
- `HealthEndpointTest` (6) starts the real application with no profile, `dev`, and `prod`, and checks over HTTP that
  health is UP, that details appear only with `dev`, that the probes answer, and that `/actuator/env` returns 404.
- `ProductRepositoryTest` (13) runs against real MySQL: products round-trip, timestamps are stored in UTC, duplicate
  SKUs and six kinds of invalid rows are rejected by the database itself, and optimistic locking refuses an update
  based on an outdated version.
- `DemoDataLoaderTest` (2) checks that the demo catalog loads only with `dev`, only into an empty table, and only once.

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
