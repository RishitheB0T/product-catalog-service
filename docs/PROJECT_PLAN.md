<!--
BUILD FLOW 01.01 — docs/PROJECT_PLAN.md
First introduced: Phase 01
Purpose: The single canonical plan: working agreement, requirements, verified version
         baseline, and the 16 build phases with their commits.
Depends on: The project requirements, restated here in full so the repository is
            self-contained.
Read by: Anyone working on the project (before each phase) and anyone checking what
         "done" means for this project.
Runtime role: None. Documentation only; no program reads this file.
-->

# FORM Catalog — Canonical Project Plan

This is the **only** project plan; the other documents link here instead of repeating it. Current state lives in [`PROGRESS.md`](PROGRESS.md), what was actually built and
checked lives in [`BUILD_GUIDE.md`](BUILD_GUIDE.md), and the reasons behind choices live in
[`DECISIONS.md`](DECISIONS.md).

The plan may change, but never silently: record every change and its reason in `DECISIONS.md`.

**Contents:** 1 Goal · 2 Working rules · 3 Honest claims · 4 Stack and versions · 5 Repository layout ·
6 BUILD FLOW comments · 7 Backend · 8 Search and SQL performance · 9 Redis caching · 10 Frontend ·
11 Testing, benchmarks, operations · 12 Git and GitHub · 13 Documentation set · 14 The 16 phases ·
15 Definition of done

<!-- BUILD FLOW 01.01.01 — Goal -->
## 1. Goal

Build a real, working product catalog from an empty folder:

1. A layered REST API: the **controller** handles HTTP, the **service** applies business rules and owns
   transactions, and the **repository** talks to the database.
2. A MySQL schema with useful constraints, paginated filtered search, measured query-plan analysis, and
   indexes justified by those measurements.
3. Redis caching for frequently requested product details, with an explicit TTL and invalidation after
   successful database commits.
4. A polished React storefront, **FORM — The Everyday Edit**, connected to the real API.
5. Docker workflows for development and production, plus a Linux EC2 deployment runbook.
6. A real Git history with at least one meaningful commit per phase (16 or more), visible on GitHub.
7. Teaching comments and documentation in construction order, so that every file can be explained and
   defended in an interview.

<!-- BUILD FLOW 01.01.02 — Working rules -->
## 2. Working rules

**Pace.** Work through the 16 phases in §14 **one phase at a time**: build it, verify it, document it, and commit
it. A phase that is too large to verify clearly is split into smaller steps; extra meaningful commits are welcome.

**Before editing.** Inspect the folder, Git state, installed tools, and operating system. Preserve existing work.
Never create a nested Git repository or overwrite an existing application without checking.

**Commands.** Record every meaningful command in `BUILD_GUIDE.md` with what it does and why. Commands for the
Windows development machine use Windows PowerShell 5.1 syntax (no `&&`; Bash is not assumed). Commands that run
inside Linux containers or on EC2 are labeled as such.

**Decisions.** Record important choices, the alternatives considered, and the trade-offs in `DECISIONS.md`.

**Missing tools or access.** Finish the work that does not depend on them and mark the dependent step **pending**
in `PROGRESS.md`. Never claim that a command, test, benchmark, browser check, push, or deployment happened when it
did not.

**Scope.** This is a catalog service with a good frontend that demonstrates it. Keep the code understandable.
Do **not** add any of the following: an Express/Node backend (Node.js is frontend tooling only), microservices,
Kubernetes, Kafka, Elasticsearch, payments, checkout or cart, a full authentication platform, Lombok, or unrelated
dependencies.

**Progress.** Whenever work stops, update `PROGRESS.md` with the current phase, last real commit, checks run,
blockers, and the precise next action.

<!-- BUILD FLOW 01.01.03 — Honest claims -->
## 3. Honest-claims rules

- Figures often quoted for this kind of cache, roughly **120 ms before caching and 15 ms after**, are
  *unverified example numbers*, not targets. We measure our own results and publish only what we measure.
- Never add artificial delay, fabricate traffic, report a cache hit that was not measured, or edit results to fit
  a story. If an optimization shows little gain or a regression, report that and investigate it.
- Containers give **reproducible images with environment-specific configuration**. Never claim that local and
  production environments are literally identical.
- A single EC2 instance running the app, database, and cache is **not highly available**. Say so.
- Cache-aside with eviction and a TTL does **not** guarantee zero stale reads (see §9). Never claim
  production-grade distributed consistency unless it is implemented and tested.
- Say "verified" only about checks that actually ran. Otherwise say "implemented, not verified" or "pending".
- Final resume bullets use measured results and completed work only.

<!-- BUILD FLOW 01.01.04 — Stack and verified version baseline -->
## 4. Stack and verified version baseline

**Stack**
- Backend: Java 21, Maven Wrapper, Spring Boot (Web MVC, Data JPA, Bean Validation, Actuator), MySQL 8.4 LTS,
  Flyway with its MySQL module, Spring Data Redis, JUnit, focused Spring test slices, and Testcontainers for
  real MySQL and Redis in tests.
- Frontend: React with JavaScript, Vite, plain CSS organized around design tokens, and the browser's native
  `fetch`. Any additional frontend library needs a concrete, recorded justification.
- Delivery: Docker Compose. In production, Nginx serves the built React files and proxies `/api` to Spring Boot.
  A small k6 benchmark suite and GitHub Actions.
- Money: `BigDecimal` in Java and `DECIMAL` in MySQL, shown as INR in the UI. No checkout or payments.

**Version rules.** Pin exact versions. Never use milestones, release candidates, snapshots, or floating container
tags such as `latest` or `8.4`. Save direct npm dependencies with exact versions and commit every lockfile.
Re-check each version in the phase that pins it. If a newer *patch* release exists, take it and record the
change in `DECISIONS.md`.

**Baseline verified on 2026-10-20.** Sources: start.spring.io metadata and docs.spring.io system requirements;
the `spring-boot-dependencies` 4.1.1 POM on Maven Central; maven.apache.org; the nodejs.org release index and
the Node.js release schedule; the npm registry; Docker Hub official images; nginx.org; GitHub release pages;
MySQL Connector/J release notes; the Testcontainers documentation.

| Component | Planned version | Notes | Pinned in (phase) |
|---|---|---|---|
| JDK | 21 (LTS) | Spring Boot 4.1.1 supports Java 17 through 26 | `pom.xml` (02) |
| Spring Boot | 4.1.1 | Current GA release. 4.2.0 is still a milestone, so it is excluded | `pom.xml` (02) |
| Spring Framework | 7.0.9 | Managed by Spring Boot | — |
| Maven / Maven Wrapper | 3.10.0 / 3.3.4 | Apache calls 3.10.0 the recommended release; Maven 4 is still a release candidate | wrapper properties (02) |
| Hibernate ORM | 7.4.5.Final | Managed by Spring Boot | — |
| Flyway | 12.4.0 plus `flyway-mysql` | Spring Boot docs require the MySQL module for MySQL | `pom.xml` (04) |
| MySQL Server | 8.4.11 (8.4 LTS line) | Official image `mysql:8.4.11` | Compose (03) |
| MySQL Connector/J | 9.7.0 | Managed; release notes: usable with MySQL Server 8.0 and later | — |
| Redis | 8.10.2 | Official image `redis:8.10.2-alpine` | Compose (03) |
| Lettuce (Redis client) | 7.5.2.RELEASE | Managed by Spring Boot | — |
| Jackson | 3.1.5 | Spring Boot 4 default. Spring Data Redis JSON serializers: `JacksonJsonRedisSerializer`, `GenericJacksonJsonRedisSerializer` | — |
| Testcontainers | 2.0.5 | 2.x renamed modules: `testcontainers-mysql`, `testcontainers-junit-jupiter` | `pom.xml` (04) |
| JUnit Jupiter | 6.0.3 | Managed by Spring Boot | — |
| Node.js for builds and CI | 26.13.0 (LTS "Lithium") | Re-checked 2026-10-30: Node 26 became the active LTS line on 2026-10-28 (Node 24, now 24.22.2, is the older LTS line). The development machine has 26.8.1, from the same line (D-053) | Dockerfile, CI (14) |
| React / React DOM | 19.3.1 | npm registry; unchanged on 2026-10-30 | `package.json` (12) |
| Vite | 8.3.5 | Re-checked 2026-10-30: 8.3.4 → 8.3.5, a patch (D-053). Files written by hand after reading the `create-vite` 9.3.3 template; `@vitejs/plugin-react` 6.1.2 | `package.json` (12) |
| Vitest / jsdom | 5.0.5 / 30.1.3 | Testing Library: react 16.3.3, dom 10.4.2 (required by react), jest-dom 7.0.1, user-event 14.6.8 | `package.json` (12) |
| oxlint | 1.90.0 | Replaces ESLint: the current Vite template (`create-vite` 9.3.3) lints with oxlint (D-050) | `package.json` (12) |
| React Router | 8.5.0 | Candidate only. Not needed in Phase 12 (one page, D-050); decided in Phase 13 | (13) |
| Nginx | 1.30.6 (stable line) | nginx.org stable release; image variant chosen in Phase 14 | Dockerfile (14) |
| Java runtime image | `eclipse-temurin:21.0.12.1_1-jre-noble` | The build stage uses the matching `-jdk-noble` image | Dockerfile (14) |
| k6 | 2.3.0 | GitHub's "latest" release. Run from the official `grafana/k6:2.3.0` image | scripts (11) |
| GitHub Actions | checkout 7.0.1, setup-java 6.1.0, setup-node 7.1.0, upload-artifact 7.0.2 | Pinning style (tag or commit SHA) decided in Phase 14 | `ci.yml` (14) |

<!-- BUILD FLOW 01.01.05 — Repository layout -->
## 5. Repository layout

```text
backend/              Spring Boot service (Maven Wrapper project)                    from Phase 02
frontend/             React + Vite storefront "FORM — The Everyday Edit"              from Phase 12
infra/                Docker Compose files, Nginx configuration, EC2 helpers          from Phase 03
scripts/              PowerShell helpers for the Windows machine; Bash only for       from Phase 03
                      scripts that run on Linux (containers, CI, EC2)
benchmarks/           Benchmark seed tooling, k6 scripts, raw results, query plans    from Phase 08
docs/                 Plan, guides, decisions, evidence                               Phase 01
.github/workflows/    Continuous integration                                          Phase 14
README.md             What works now, how to run it, evidence, deployment status      Phase 01
```

- Host ports: a project-specific 24xxx block, bound to `127.0.0.1` for local services (README "Ports", decision
  D-016). Framework defaults such as 8080, 3306, and 6379 are never bound on the development machine.
- Planned Java base package: `com.example.catalog`. The `example.com` domain is reserved for documentation, so
  this package never claims a domain we don't own. It is confirmed in Phase 02.
- Planned code organization: one feature package (`product`) whose class names show the layer
  (`ProductController`, `ProductService`, `ProductRepository`), plus small shared packages such as `config` and `error`.

<!-- BUILD FLOW 01.01.06 — BUILD FLOW numbering and teaching comments -->
## 6. BUILD FLOW numbering and teaching comments

Source comments are a core deliverable of this learning project.

**Numbers.** Every human-authored file gets a stable BUILD FLOW number when it is first created. `PP.NN` means
the `NN`-th file created in phase `PP`. Additional digits number logical sections inside a file: `05.03.02` is
the second section of the third file introduced in Phase 05.
- The number lives in a top-of-file comment, **never** in a class name or filename.
- A file keeps its original number permanently. Later changes are recorded in `BUILD_GUIDE.md` (and in the file's
  own comments), not by renumbering.

**Header template** (written in each format's own comment syntax):

```java
/*
 * BUILD FLOW 05.03 — ProductService.java
 * First introduced: Phase 05
 * Purpose: Apply product rules before talking to the database.
 * Depends on: ProductRepository, ProductMapper, product DTOs.
 * Called by: ProductController.
 * Runtime role: The business-logic part of an API request.
 */
```

**Section template:**

```java
// BUILD FLOW 05.03.02 — Updating a product
// We load the existing record so a missing ID produces a clear 404 response.
// The transaction keeps the database change together.
```

| Format | Comment syntax | Placement notes |
|---|---|---|
| Java, JavaScript, JSX, CSS | `/* … */`; `//` in Java and JS | Inside JSX markup, use `{/* … */}` |
| SQL (MySQL, Flyway) | `-- ` | Flyway checksums cover the whole file, comments included, which is one more reason never to edit an applied migration |
| YAML, `.properties`, Dockerfile, Nginx, `.gitignore`, `.gitattributes`, shell, PowerShell | `#` | A shebang (`#!/usr/bin/env bash`) stays on line 1 and the header follows it. PowerShell `param()` blocks keep their required position |
| XML (`pom.xml`), HTML, Markdown | `<!-- … -->` | An XML declaration, if present, stays first |
| JSON, lockfiles, generated wrappers, binaries, vendored or machine output | None. Never inject comments | Documented in `FILE_GUIDE.md` instead. Lockfiles are never hand-edited |

**What to comment.** Cover every meaningful annotation, method, component, query, configuration group, algorithm,
condition, and error-handling branch. State its inputs, outputs, and dependencies, and *why* the approach is needed.
Go deeper on dependency injection, DTOs, transactions, indexes, caching, React state and effects, HTTP calls, and
container networking. Explain unfamiliar syntax the first time it appears; later uses may point back to that
explanation. Don't comment braces or restate obvious code in English. When behavior changes, update the comment in
the same commit, so no comment describes behavior that no longer exists. Never put passwords, tokens, or personal
machine paths in comments or docs. Avoid Lombok and unnecessary code generation so the important behavior stays visible.

**Runtime step IDs** are a separate numbering for what happens *while a request runs*: `SEARCH-01…`, `READ-01…`,
and `WRITE-01…`, defined in `REQUEST_FLOWS.md`. Code comments may reference them, for example `// Runtime: READ-04`.
BUILD FLOW numbers record when a file was written; runtime IDs record when code executes.

**Guides kept current every phase**
- `FILE_GUIDE.md` has one row per tracked file: BUILD FLOW number, exact path, purpose, dependencies,
  caller/consumer, and when to read it. Generated files are marked, and binary or generated internals are grouped.
- `BUILD_GUIDE.md` covers each phase: what we had before, what we add, why, files in order, relevant syntax,
  commands, expected behavior, actual checks, and how the new pieces connect.

<!-- BUILD FLOW 01.01.07 — Backend requirements -->
## 7. Backend requirements

**One aggregate: `Product`.** Fields: `id`, a unique `sku`, `name`, `description`, `category`, `price`,
`stockQuantity`, `imageUrl`, `createdAt`, `updatedAt`, and a `version` column for optimistic locking, which
prevents lost updates. The category is a small constrained set (planned: a Java enum plus a database `CHECK`
constraint); there is no category-management service. Rules are enforced twice: by request validation **and**
by database constraints (unique SKU, `NOT NULL`, non-negative price and stock, length limits).

**Endpoints**

| Method | Path | Success response | Purpose |
|---|---|---|---|
| GET | `/api/v1/products` | 200 | Paginated, filtered search |
| GET | `/api/v1/products/{id}` | 200 | Product detail (cached from Phase 09) |
| POST | `/api/v1/products` | 201 with a `Location` header | Create a product |
| PUT | `/api/v1/products/{id}` | 200 | Update a product (optimistic locking) |
| DELETE | `/api/v1/products/{id}` | 204 | Delete a product |

**Search parameters:** a name query (contains match), category, minimum and maximum price, an in-stock flag,
a zero-based `page`, a bounded `size` (maximum 100), and `sort` from a small documented allowlist. API page 0
is shown as "page 1" in the UI.

**Rules**
- Validate price ranges (non-negative, min ≤ max), page/size limits, sort values, text lengths, stock, and required fields.
- Stable ordering: every sort ends with `id` as a tie-breaker, so pages never overlap or skip rows.
- Never build SQL from untrusted sort or filter strings. Map allowlisted values to sorts defined in code and use bound parameters.
- Return DTOs, never JPA entities.
- Paginated response shape: `content`, `page`, `size`, `totalElements`, `totalPages`.
- Use correct HTTP statuses and one consistent error body, including field errors for validation failures.
- Map duplicate SKUs and optimistic-lock conflicts to understandable `409 Conflict` responses.
- Use explicit mapping code and constructor injection. Don't create an interface for every class without a reason.
- Transactions live at the service boundary. Comments explain where each one begins, commits, or rolls back.
- Flyway is the schema authority and Hibernate only validates the schema (`ddl-auto=validate`). Never use `ddl-auto=update` in production.
- Seed demo data only through an explicit dev/test mechanism. Large benchmark seeding is separate and opt-in.
- **Read-only public deployment.** The public EC2 deployment rejects writes on the server, not just by hiding
  buttons, with 403 Forbidden (decision D-026). Local admin CRUD works. Public editing would first require real
  server-side authentication.
- Never put secrets in Vite-exposed environment variables or credentials in the frontend.

<!-- BUILD FLOW 01.01.08 — Search and SQL performance requirements -->
## 8. Search and SQL performance requirements

Build correct search first. Then measure. Then optimize.
- Capture the actual SQL generated for representative search requests.
- Use a deterministic, configurable benchmark dataset (about 100,000 products if the machine can handle it).
  Keep the normal demo fixture small.
- Save `EXPLAIN ANALYZE` output for a genuinely relevant filter/sort query **before and after** a justified query
  or index change.
- Explain in simple words: composite-index column order, the leftmost-prefix rule, filtering versus sorting, and
  the write and storage cost of every index. An index on every column is not automatically useful.
- Explain why a contains search with a leading wildcard (`LIKE '%term%'`) cannot use an ordinary B-tree index to
  seek. Never silently turn a contains search into a prefix search and call it the same behavior.
- Choose query rewrites only after reading the plan, and prove that results are unchanged.
- Explain why offset pagination gets slower at deep pages. Keyset pagination is a documented extension, not a requirement.
- Never modify an applied Flyway migration to add an index. Add a new migration.
- Keep before/after evidence without dropping production indexes or resetting production data.
- Document the SQL, dataset, plans, observed changes, and limits in `docs/QUERY_OPTIMIZATION.md` (Phase 08).

<!-- BUILD FLOW 01.01.09 — Redis caching and consistency requirements -->
## 9. Redis caching and consistency requirements

- At first, cache **only the GET-by-id product response DTOs**. Search stays database-backed, which avoids a large
  search-cache invalidation problem.
- Cache-aside: check Redis; on a miss, read MySQL and store the response with a TTL.
- Use a versioned, application-specific key namespace with a documented format (planned: `catalog:v1:product:{id}`)
  and explicit JSON serialization that is compatible with Spring Boot 4 and Jackson 3.
- Don't cache missing products (initially).
- Use a configurable short TTL (for example 60 seconds) and explain the trade-off between freshness and hit rate.
- After a successful create, update, or delete transaction, invalidate the affected detail key. For create, use
  the actual generated id.
- Invalidation must genuinely happen **after commit**, using a transaction-aware cache or an explicit after-commit
  listener, and we explain the chosen mechanism. Default `@CacheEvict` timing does not prove after-commit ordering.
  If caching annotations are used, explain Spring proxies and self-invocation.
- If Redis is unavailable, use short timeouts and a documented database fallback for reads.
- Handle cache failures separately from business exceptions. Never broadly swallow all exceptions.
- Never report a committed write as rolled back just because the eviction that followed it failed. Log and count
  that failure, and document the remaining freshness risk.
- No `FLUSHALL`, no broad production cache deletion, and no public cache-bypass or debug endpoint.
- Be honest about limits: cache-aside with eviction and a TTL can still serve stale data during concurrent
  miss/write races or Redis failures. Document a concrete example race. Explain cache stampedes and stronger
  approaches as extensions only.
- Verify against real Redis: hit/miss, serialization, update/delete invalidation, rollback behavior, TTL expiry,
  and fallback during a Redis outage. Don't write tests that only assert an annotation exists.
- A configuration switch can disable caching, so benchmarks compare the **same endpoint** with the cache off and on.
- Document key and TTL choices, write flows, failure behavior, and consistency limits in `docs/CACHING.md` (Phases 09–10).

<!-- BUILD FLOW 01.01.10 — Frontend requirements -->
## 10. Frontend requirements — "FORM — The Everyday Edit"

Visual design is a real deliverable. Direction: a warm off-white background, charcoal type, a restrained sage
accent, generous spacing, crisp borders, strong editorial headings, good product photography, a clear type scale
and spacing system, and consistent components. Avoid a page made entirely of oversized rounded cards.

**Must include**
- A refined header and navigation, a compact editorial hero, search, categories, and a responsive product grid.
- Filter controls on desktop and a usable filter drawer on mobile, active filter chips with a clear-all action,
  sorting, pagination, and accurate result counts.
- A product detail view showing the image, description, price, stock, and SKU.
- An admin view for local development: create, edit, and delete products, with form validation and delete confirmation.
- Loading skeletons, empty states, helpful API error messages, image fallbacks, and success feedback.
- Accessibility: keyboard navigation, visible focus, labeled inputs, accessible dialog behavior, sufficient
  contrast, and reduced-motion support.
- Layouts that work on mobile (about 375 px), tablet, and desktop (about 1440 px).
- Debounced search, cancellation or safe handling of stale requests, page reset when filters change, and filter
  state stored in the URL so a search can be refreshed and shared.
- Consistent INR price display.

**Rules**
- Use real API data. A development-only mock may be used briefly while styling, then removed from the runtime.
- Never fake ratings, cache metrics, traffic, benchmark graphs, or stock status.
- Use legally usable product images and document their source and license. The UI must stay usable when images fail.
- No fake checkout or cart. Keep implementation details off customer screens; explanations belong in comments and docs.
- Keep API calls, components, pages, and styles separate, without an elaborate architecture. Explain props, state,
  effects, rendering, fetch, and error handling as they appear.
- Verify in a real browser, using automation if it is available. Otherwise provide a concrete manual checklist and
  mark those checks **unverified**.

<!-- BUILD FLOW 01.01.11 — Testing, benchmarks, and operations requirements -->
## 11. Testing, benchmarks, and operations requirements

**Testing**
- Tests arrive with the feature they protect, not at the end. Phase 07 *broadens* the tests; it is not
  permission to leave Phases 04–06 untested.
- Use Testcontainers (real MySQL and Redis) wherever behavior depends on those systems.
- Cover validation, missing records, unique SKUs, combined filtering, sort tie-breakers, pagination, transaction
  outcomes, cache invalidation and fallback, and production read-only settings rejecting writes.
- Add a small number of frontend tests for behavior that could realistically break. Don't chase a coverage percentage.

**Benchmarks**
- Write reproducible k6 scripts. An application configuration switch compares cache disabled and enabled on the
  same endpoint.
- Report product-detail caching results separately from search and index optimization results.
- Warm the JVM and the database for both comparisons, and warm Redis only for the warm-cache scenario. Report cold
  behavior separately.
- Keep the dataset, request mix, machine, concurrency, duration, and network path controlled, and record them.
- Run multiple comparable trials. Report mean, median, p95, throughput, error rate, and exact sample and run counts.
- Save raw results and write `docs/PERFORMANCE.md`. Never compare one cold uncached request with one warm cached
  request as proof of general performance. Keep developer-only measurement controls away from public endpoints.

**Containers and CI**
- Multi-stage Docker builds, meaningful health checks, persistent MySQL storage, environment-based configuration,
  and a non-root application process.
- Use the same application image for local production-style verification and for EC2, with environment-specific
  configuration.
- Locally, the Vite dev server proxies `/api`. In production, Nginx serves the built frontend and proxies `/api`.
- MySQL and Redis stay inside the Compose network. If a host port is needed locally, bind it to `127.0.0.1`.
- Limit Actuator exposure and keep sensitive diagnostics private.
- Provide `.env.example` with placeholders. Real `.env` files are git-ignored.
- GitHub Actions runs the backend checks and the frontend checks and build. Document what CI integration tests need.
- Rebuild from a clean checkout to prove the build is reproducible.

**EC2**
- Prepare the scripts and runbook *before* asking for access.
- Before creating paid AWS resources, document the instance size, region, and current expected cost, and approve that concrete choice
  explicitly before launching anything. Assume nothing is free.
- Use credentials only through the provider's normal secure login flow. Never share long-lived keys or commit them.
- Cover security groups, SSH restricted to the developer's own IP, Docker installation, configuration, startup, logs,
  health checks, updates, restart, backups, restore, rollback, and cleanup.
- Keep database and cache ports private. Use HTTPS for a publicly shared deployment (and explain the domain
  requirement), or an SSH tunnel for a private demo.
- Document the limits of running the app, database, and cache on one instance. Never call it highly available.
- When access exists, verify the deployed URL, health, and an API/UI smoke check. Otherwise mark deployment
  **pending** and never describe the project as deployed.

<!-- BUILD FLOW 01.01.12 — Git and GitHub rules -->
## 12. Git and GitHub rules

Make real commits as work happens, on the `main` branch, and push each one to the GitHub remote. Never force-push,
rewrite or amend published history, make empty or backdated commits, split a finished repository into artificial
history, or commit credentials.

**Before every commit**
1. Inspect `git status` and the relevant diff.
2. Run the checks appropriate to the phase.
3. Update `FILE_GUIDE.md`, `BUILD_GUIDE.md`, `PROGRESS.md`, and `LEARNING_LOG.md`, plus any other affected docs.
4. Stage explicit paths (never a blind `git add .`), scan the staged diff for secrets, and commit with a meaningful message.
5. Record the short hash in `PROGRESS.md` with the next commit (a commit cannot contain its own hash).
6. Push to `origin`, then verify that GitHub's `main` points at the new commit: `git ls-remote origin refs/heads/main`
   must print the same hash as `git rev-parse HEAD`. If a push fails, mark it pending in `PROGRESS.md` and keep
   the commit local. Never report a push that did not succeed.

A phase commit is complete when that phase's own feature, configuration, and docs can be verified. Never claim
the unfinished project is complete.

**GitHub remote.** `origin` is https://github.com/RishitheB0T/product-catalog-service (public, default branch
`main`), created with the GitHub CLI on 2026-10-20 (decision D-009). Every commit is pushed right after it is made.
Authentication uses normal secure flows only: Git Credential Manager for `git push`, and the GitHub CLI's browser
device flow for `gh`. Tokens stay in the Windows credential store, never in files or commits. Commits stay local
until a push succeeds.

<!-- BUILD FLOW 01.01.13 — Documentation set -->
## 13. Documentation set

Each phase is written up in `BUILD_GUIDE.md` under the same headings: what we had before, what we add, why, the
files in order, relevant syntax, commands, expected behavior, actual checks, and how the new pieces connect.

**Documents, maintained progressively.** Each is created when it has real content; no empty placeholders.

| Document | Holds | Created in |
|---|---|---|
| `README.md` | What works, architecture, quick start, API/UI usage, test commands, performance evidence, deployment status | 01 |
| `docs/PROJECT_PLAN.md` | This plan | 01 |
| `docs/BUILD_GUIDE.md` | Construction and reading order, phase by phase | 01 |
| `docs/FILE_GUIDE.md` | Every tracked file explained | 01 |
| `docs/REQUEST_FLOWS.md` | Numbered runtime flows: search, uncached detail, cache-hit detail, successful write, rollback, Redis outage | 01 (planned flows); real from 05 |
| `docs/DECISIONS.md` | Important choices and their trade-offs | 01 |
| `docs/LEARNING_LOG.md` | Concepts learned and small exercises per phase | 01 |
| `docs/PROGRESS.md` | Current phase, last commit, checks run, blockers, next action | 01 |
| `docs/QUERY_OPTIMIZATION.md` | Actual SQL and before/after query plans | 08 |
| `docs/CACHING.md` | Key design, TTL, transaction ordering, failures, consistency limits | 09 |
| `docs/PERFORMANCE.md` | Reproducible method, raw-result references, measured numbers | 11 |
| `docs/DEPLOYMENT.md` | Local production-style run, EC2 runbook, verification, operations, cleanup, pending steps | 14 |
| `docs/INTERVIEW_GUIDE.md` | 60-second explanation, five-minute walkthrough, likely questions, evidence-backed resume bullets | by 16 |

<!-- BUILD FLOW 01.01.14 — The 16 phases -->
## 14. The 16 phases

| Phase | Build result | Commit message |
|---|---|---|
| 01 | Workspace checks, architecture, numbered build plan | `docs: define catalog architecture and phased build plan` |
| 02 | Maven/Spring Boot skeleton, profiles, basic health check | `chore(backend): bootstrap Spring Boot application` |
| 03 | Local MySQL/Redis Compose services and environment examples | `chore(infra): add local MySQL and Redis services` |
| 04 | Flyway schema, entity, repository, small demo fixture | `feat(database): add product schema and persistence` |
| 05 | DTOs, mapping, transactional CRUD API, focused checks | `feat(api): implement product CRUD operations` |
| 06 | Filtered search, stable pagination and sorting, validation, errors | `feat(search): add validated product search and pagination` |
| 07 | Broader API/MySQL integration tests and example requests | `test(backend): verify catalog API and database behavior` |
| 08 | Query-plan baseline, measured index/query improvements, new migration | `perf(database): optimize measured catalog search queries` |
| 09 | Redis detail-read caching, keys, JSON serialization, TTL | `feat(cache): cache product details in Redis` |
| 10 | After-commit invalidation, outage handling, consistency tests | `feat(cache): handle committed writes and Redis failures` |
| 11 | Repeatable seeding and benchmarks, actual performance report | `perf: add reproducible catalog benchmarks and evidence` |
| 12 | Frontend design system, responsive catalog, real API search | `feat(frontend): build responsive product catalog` |
| 13 | Detail and admin views, accessibility, interaction checks, visual polish | `feat(frontend): add product management and polish interactions` |
| 14 | Production image builds, Nginx, complete Compose stack, CI | `build: add production containers and continuous integration` |
| 15 | EC2 deployment scripts, operational checks, runbook; deploy if access exists | `chore(deploy): add EC2 deployment and operations workflow` |
| 16 | Complete teaching guide, evidence review, interview walkthrough, GitHub verification | `docs: finalize project walkthrough and verified results` |

The phase details below list planned files by name only. BUILD FLOW numbers are assigned in creation order when
each file is actually written, and `FILE_GUIDE.md` is the record. Approaches marked *planned* are confirmed in
their phase, or changed with a `DECISIONS.md` entry.

### Phase 01 — Workspace, architecture, and plan
- Inspect the folder, Git, OS, and tools. Verify the version baseline (§4) against official sources.
- Write this plan, `.gitignore`, `.gitattributes`, `README.md`, and the first versions
  of `BUILD_GUIDE`, `FILE_GUIDE`, `REQUEST_FLOWS` (planned flows), `DECISIONS`, `LEARNING_LOG`, and `PROGRESS`.
- Run `git init -b main` and make the first commit.
- Checks: ignore and line-ending rules behave as intended, `FILE_GUIDE.md` lists every tracked file, documentation
  links resolve, and the staged diff contains no secrets.

### Phase 02 — Spring Boot skeleton
- Generate `backend/` with Spring Initializr (Spring Boot 4.1.1, Java 21, Maven, jar) using **only** Web MVC,
  Validation, Actuator, and their test starters. Database and Redis dependencies arrive in the phases that use
  them, so the application can start without any services.
- Pin the Maven Wrapper to Maven 3.10.0. Add the main class, `application.yml`, `dev` and `prod` profiles, and
  limit Actuator exposure to health.
- Checks: `.\mvnw.cmd --version`, `.\mvnw.cmd test` (including a test that calls `/actuator/health`), then run the
  application and call the health endpoint with `curl.exe`.

### Phase 03 — Local MySQL and Redis
- An `infra/` Compose file for `mysql:8.4.11` and `redis:8.10.2-alpine`: a named volume for MySQL data, health
  checks, and host ports bound to `127.0.0.1` only. Add `.env.example` with placeholders and small PowerShell helpers.
- Checks: containers report healthy, `SELECT VERSION()` works, `PING` returns `PONG`, ports listen on `127.0.0.1`
  only, and `.env` stays ignored.

### Phase 04 — Schema and persistence
- Add Data JPA, Flyway with `flyway-mysql`, MySQL Connector/J, and Testcontainers.
- Write the `V1` migration (primary key, unique SKU, `NOT NULL`, non-negative price and stock, category allowlist,
  lengths, timestamps, version), the `Category` enum, the `Product` entity (no Lombok), and `ProductRepository`.
  Set `ddl-auto=validate`.
- Add a small demo fixture (about 24–36 products) applied only under the dev profile. Built as a dev-only loader
  that fills an empty table, not as a Flyway migration (decision D-022).
- Checks: Testcontainers repository tests (migration, schema validation, constraint violations), the application
  starting against Compose MySQL, and an inspection of `flyway_schema_history`.

### Phase 05 — CRUD API
- Add request and response DTOs (Java records) with Bean Validation, an explicit mapper, `ProductService` with
  `@Transactional` boundaries, and `ProductController` for GET by id, POST, PUT, and DELETE.
- Return one consistent error body: field errors, 404, and 409 for duplicate SKUs and optimistic-lock conflicts.
- Add a server-side write switch, off by default in production configuration, with tests.
- Checks: web-slice tests for validation and errors, service tests, and manual CRUD with `curl.exe`.

### Phase 06 — Search
- `GET /api/v1/products` with `q`, `category`, `minPrice`, `maxPrice`, `inStock`, `page`, `size` (at most 100),
  and an allowlisted `sort`. Build dynamic predicates with bound parameters (Spring Data JPA Specifications, decision
  D-031), always add the `id` tie-breaker, and return `content`, `page`, `size`, `totalElements`, `totalPages`.
- Checks: tests for combined filters, ordering ties, page boundaries, and invalid parameters returning 400.

### Phase 07 — Broader backend tests
- Full-stack integration tests (random port, real MySQL) for CRUD, validation, 404s, 409s, combined filters,
  sort tie-breakers, pagination, transaction rollback, and read-only rejection.
- Example requests: PowerShell/`curl.exe` commands plus an `.http` file.
- Checks: `.\mvnw.cmd verify`, with the test summary recorded.

### Phase 08 — Measured query optimization
- Deterministic, configurable, opt-in benchmark seed tooling (about 100,000 products).
- Capture the actual SQL and save `EXPLAIN ANALYZE` **before** any change. Choose changes from the plan, add a new
  `V2__…` migration, save the plan **after**, and prove the results are identical.
- Write `docs/QUERY_OPTIMIZATION.md`.
- Checks: saved plans are committed, tests stay green, and a result-equivalence check passes.

### Phase 09 — Redis detail caching
- Add Spring Data Redis. Implement cache-aside for product-detail DTOs (planned: explicit service-level code rather
  than annotations, so behavior and failures stay visible), with the key `catalog:v1:product:{id}`, JSON for the
  DTO type, a configurable 60-second TTL, a cache on/off switch, and hit, miss, and error metrics.
- Write `docs/CACHING.md`.
- Checks: Testcontainers Redis tests (miss then store, hit, serialization round trip, TTL expiry), and key/TTL
  inspection with `redis-cli`.

### Phase 10 — Committed writes and Redis failures
- Evict after commit for create, update, and delete (planned: a transactional event listener in the `AFTER_COMMIT` phase).
- A rollback causes no eviction and no data change. Use short Redis timeouts and fall back to MySQL for reads.
  Log and count eviction failures without misreporting the committed write.
- Document the stale-read race and cache stampedes in `docs/CACHING.md`.
- Checks: tests for invalidation, rollback, fallback during a Redis outage, and the eviction-failure path.

### Phase 11 — Benchmarks and evidence
- k6 2.3.0 scripts, run from the official Docker image, for product detail (cache off and on, same endpoint) and
  for search. Use a warm-up protocol, report cold runs separately, run multiple trials, and save raw results.
- Write `docs/PERFORMANCE.md` with mean, median, p95, throughput, error rate, sample and run counts, the machine,
  and the method.
- Checks: every reported number traces back to a raw result file.

### Phase 12 — Frontend catalog
- Vite with React (JavaScript) and design tokens. Build the header, hero, search, categories, responsive grid,
  desktop filters, mobile drawer, chips with clear-all, sorting, pagination, and counts. Add debounced search,
  stale-request cancellation (`AbortController`), URL-backed state, INR formatting, skeleton/empty/error states,
  image fallbacks, documented image licenses, the Vite dev proxy, and a few behavior tests.
- Checks: lint, tests, and a production build, plus browser checks at about 375 px, tablet, and about 1440 px
  (automated if available, otherwise a manual checklist marked unverified).

### Phase 13 — Detail, admin, accessibility, polish
- The product detail view, plus local admin create/edit/delete with validation, delete confirmation, and success
  feedback. The UI respects the server's write switch, and the server still enforces it.
- Keyboard navigation, visible focus, labeled inputs, accessible dialogs, contrast, and reduced motion.
- Checks: frontend tests and browser checks of CRUD, keyboard paths, and mobile layouts.

### Phase 14 — Production containers and CI
- Multi-stage Dockerfiles (with a non-root backend process), Nginx configuration (static files, SPA fallback,
  `/api` proxy), and a production Compose file (only Nginx published, MySQL and Redis internal, a persistent
  volume, health checks), with environment-based configuration and limited Actuator exposure.
- GitHub Actions: backend `verify` (including Testcontainers) and frontend lint, test, and build.
- Write `docs/DEPLOYMENT.md` (local production-style run).
- Checks: the stack is healthy, smoke tests pass through Nginx, writes are rejected in read-only mode, a clean
  checkout rebuilds, and CI is green on GitHub. Also check the built artifact itself: the image's jar contains its
  configuration files, and production-only settings are visibly active (lesson from Phase 02, when tests passed
  but a jar built next to an editor's background build was missing its YAML files).

### Phase 15 — EC2
- Write the scripts and runbook first. Then propose the instance, region, and cost for approval, and deploy only
  when access exists.
- Cover the security group, SSH from the developer's own IP only, private database and cache, Docker installation,
  configuration, startup, logs, health, updates, restart, backup and restore, rollback, cleanup, HTTPS or an SSH
  tunnel, and single-instance limits.
- Checks: the deployed URL, health, and an API/UI smoke test, or the deployment marked pending.

### Phase 16 — Final walkthrough and verification
- Complete the guides, write `docs/INTERVIEW_GUIDE.md`, audit the requirements (§15), and push and verify the
  GitHub history.
- Checks: the remote branch points at the expected commit, and every claim in the README traces to evidence.

<!-- BUILD FLOW 01.01.15 — Definition of done and final audit -->
## 15. Definition of done and final audit

The project is done when the actual repository and evidence show that:
- every requirement in §7–§11 is implemented, and each is marked as **verified** (a check ran and is recorded),
  **implemented but unverified**, or **blocked** (with the missing access named);
- the Git history shows the phase commits on GitHub, or the push is explicitly marked pending;
- `README.md` states the real status, real measurements, and real deployment status;
- `docs/INTERVIEW_GUIDE.md` contains resume bullets backed only by measured results and completed work.

The final audit in Phase 16 compares this plan with the repository. It reports which features were verified,
which were only implemented, and any remaining external-access blockers. It gives the GitHub URL and history,
and a deployed URL, only if they really exist.
