<!--
BUILD FLOW 01.08 — docs/DECISIONS.md
First introduced: Phase 01
Purpose: Record important choices: the context, the decision, the alternatives, and the trade-offs.
Depends on: docs/PROJECT_PLAN.md (the requirements each decision serves).
Read by: Anyone asking "why X and not Y?", which also makes it good interview preparation.
Runtime role: None. Documentation only.
-->

# Decisions

Each entry has a status: **Accepted**, **Planned** (to be confirmed in a later phase), or **Superseded** (kept for
history, with a pointer to its replacement).

<!-- BUILD FLOW 01.08.01 — Decisions made in Phase 01 -->
## D-001 · One repository for backend, frontend, infrastructure, and docs — Accepted (Phase 01)

- **Context:** one developer, one product, and one history to show on GitHub.
- **Decision:** a single repository with `backend/`, `frontend/`, `infra/`, `scripts/`, `benchmarks/`, and `docs/`.
- **Alternative:** a separate repository for each part.
- **Trade-offs:** one commit can change the API and the UI together, and CI sees everything. In exchange, CI must
  check both stacks, and the history mixes Java and JavaScript work. Commit scopes such as `feat(api)` and
  `feat(frontend)` keep the history readable.

## D-002 · Spring Boot 4.1.1 on Java 21 — Accepted (Phase 01)

- **Context:** the requirements call for Java 21 and a current stable Spring Boot release.
- **Decision:** Spring Boot 4.1.1. It is the current GA release and start.spring.io's default on 2026-10-20, and it
  supports Java 17 through 26.
- **Alternatives:** 4.0.8 (the older GA line); 4.2.0-M2 (a milestone, which the version rules exclude); Spring Boot 3.x
  (the previous generation).
- **Trade-offs:** many online tutorials still show Spring Boot 3 names, such as `spring-boot-starter-web`,
  Jackson 2 classes, and Testcontainers 1.x artifact names. We point out these differences when they appear. For
  example, start.spring.io now generates `spring-boot-starter-webmvc` and one test starter per technology.

## D-003 · Version baseline and pinning — Accepted (Phase 01)

- **Decision:** use the exact versions in plan §4, verified on 2026-10-20. Floating tags are excluded, lockfiles
  are committed, direct npm dependencies are saved with exact versions, and each pin is re-checked in the phase
  that adds it.
- **Notable picks:**
  - MySQL **8.4.11**: the newest patch of the 8.4 LTS line that the requirements specify.
  - Redis **8.10.2**: the newest stable line. Older lines (8.8, 8.6, 8.4) still receive patches, but nothing here needs them.
  - Node.js **24.22.0 LTS** for container and CI builds. Node 26 becomes LTS on 2026-10-28 according to the
    official schedule, so this is re-checked in Phase 12. The local Node 26.8.1 meets every planned package's
    `engines` range.
  - k6 **2.3.0**: GitHub marks it as the latest release, while 1.8.x is a maintenance line.
  - Maven **3.10.0** through the wrapper: Apache's recommended release. Maven 4 is still a release candidate.
- **Trade-offs:** newer lines have fewer community examples, and pinned versions need deliberate updates. That is
  the point: no surprise upgrades.

## D-004 · Add dependencies only in the phase that uses them — Accepted (Phase 01)

- **Decision:** Phase 02 starts with Web MVC, Validation, and Actuator only. Database dependencies arrive in
  Phase 04 and Redis in Phase 09.
- **Why:** with JPA on the classpath, the application refuses to start without a database. Adding each piece when
  it is needed keeps every phase runnable and every dependency explainable.
- **Trade-off:** `pom.xml` changes in several phases. Each change is recorded in `BUILD_GUIDE.md`.

## D-005 · One canonical plan document — Accepted (Phase 01)

- **Decision:** the plan lives only in `docs/PROJECT_PLAN.md`; the README and the other guides link to it instead
  of repeating it.
- **Alternative:** spreading plan details across the README and each guide, where the copies drift apart.
- **Trade-off:** one more file to open, in exchange for a single source of truth.

## D-006 · LF line endings in the repository — Accepted (Phase 01)

- **Context:** we develop on Windows (CRLF) but run scripts in Linux containers, CI, and on EC2 (LF). A shell script
  saved with CRLF fails on Linux. This machine's Git has `core.autocrlf=true` set system-wide.
- **Decision:** `.gitattributes` stores text files with LF and checks them out with LF. `.cmd`, `.bat`, and `.ps1`
  files keep CRLF. Images, fonts, and archives are binary.
- **Trade-off:** Windows editors must handle LF files. VS Code does.

<!-- BUILD FLOW 01.08.02 — Planned decisions -->
## D-007 · The public deployment is read-only, enforced by the server — Planned (Phases 05, 14, 15)

- **Decision:** a server-side write switch, on for local development and off in production configuration. Hiding
  buttons is not security.
- **To decide in Phase 05:** the exact status code and error message for rejected writes.

## D-008 · Cache only product details first; search stays on MySQL — Planned (Phase 09)

- **Why:** a detail key maps to exactly one row, so invalidation is precise. Search results depend on many rows and
  on the filters, so caching them would need broad invalidation.

<!-- BUILD FLOW 01.08.04 — Decisions made after Phase 01 -->
## D-009 · Public GitHub repository, pushed after every commit — Accepted (2026-10-20, before Phase 02)

- **Context:** the history should be visible on GitHub as it grows, and CI (Phase 14) needs a remote.
- **Decision:** the public repository `RishitheB0T/product-catalog-service`, created with GitHub CLI 2.102.0
  (installed with winget, signed in through the browser device flow). After every commit, `origin/main` is pushed
  and verified.
- **Alternatives:** a private repository, which could be made public later; creating the repository on a
  pre-filled github.com page instead of installing the CLI.
- **Trade-offs:** everything committed is public immediately, so the secret scan before each commit matters even
  more. In return, the step-by-step history is visible, GitHub Actions is free for public repositories, and `gh`
  can check CI runs from the terminal in Phase 14.

<!-- BUILD FLOW 01.08.03 — Open questions -->
## Open questions (each is decided in its phase)

| Question | Phase |
|---|---|
| Final Java base package (`com.example.catalog` proposed) | 02 |
| Demo-data mechanism (a dev-only Flyway location proposed) | 04 |
| Error body format: Spring's `ProblemDetail` (RFC 9457) or a custom record | 05 |
| Status code for writes rejected in read-only mode | 05 |
| How the client sends the expected version: a body field or an `If-Match` header | 05 |
| Search implementation (Spring Data JPA Specifications proposed) | 06 |
| Cache code style (explicit service code proposed) and eviction mechanism (an after-commit listener proposed) | 09–10 |
| Routing: React Router or a small hand-written router | 12 |
| Product images: self-hosted licensed files or licensed remote URLs | 12 |
| Nginx image: official or unprivileged variant | 14 |
| How EC2 receives the exact same image: a registry, or `docker save` and `docker load` | 14–15 |
| EC2 instance type, region, and cost (approved before anything is launched) | 15 |
