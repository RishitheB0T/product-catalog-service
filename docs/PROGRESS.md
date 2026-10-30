<!--
BUILD FLOW 01.10 — docs/PROGRESS.md
First introduced: Phase 01
Purpose: The current state of the build: phase, last real commit, checks actually run,
         blockers, and the precise next action.
Depends on: Every other document; it summarizes where they stand.
Read by: Anyone resuming work on the project; read it first.
Runtime role: None. Documentation only.
-->

# Progress

> Read this first when resuming work, and update it whenever work stops.

<!-- BUILD FLOW 01.10.01 — Current state -->
## Current state

| Item | Value |
|---|---|
| Current phase | **12 — complete** |
| Next phase | 13 — product detail view, local admin (create, edit, delete), accessibility polish |
| Branch | `main`, tracking `origin/main` |
| Repository | https://github.com/RishitheB0T/product-catalog-service (public) |
| Last updated | 2026-10-31 |

**Commits so far** (each commit records the hash of the one before it, because a commit cannot contain its own hash)

| Step | Commit | Message | On GitHub |
|---|---|---|---|
| Phase 01 | `5be2fc3` | `docs: define catalog architecture and phased build plan` | Pushed and verified |
| Between 01 and 02 | `225433f` | `docs: connect GitHub remote` | Pushed and verified |
| Phase 02 | `ee94490` | `chore(backend): bootstrap Spring Boot application` | Pushed and verified |
| Phase 03 | `71f067c` | `chore(infra): add local MySQL and Redis services` | Pushed and verified |
| Phase 04 | `191b5b8` | `feat(database): add product schema and persistence` | Pushed and verified |
| Phase 05 | `05e70c3` | `feat(api): implement product CRUD operations` | Pushed and verified |
| Phase 06 | `c0566a4` | `feat(search): add validated product search and pagination` | Pushed and verified |
| Phase 07 | `fab9876` | `test(backend): verify catalog API and database behavior` | Pushed and verified |
| Phase 08 | `837a972` | `perf(database): optimize measured catalog search queries` | Pushed and verified |
| Phase 09 | `bc82587` | `feat(cache): cache product details in Redis` | Pushed and verified |
| Phase 10 | `2abde5e` | `feat(cache): handle committed writes and Redis failures` | Pushed and verified |
| Phase 11 | `d64f10f` | `perf: add reproducible catalog benchmarks and evidence` | Pushed and verified |
| Phase 12 | Recorded at the start of Phase 13 | `feat(frontend): build the storefront catalog` | Pushed and verified when committed |

<!-- BUILD FLOW 01.10.02 — Checks run in Phase 01 -->
## Checks run in Phase 01

All of these ran on 2026-10-20 against the staged files. Commands and full results are in `BUILD_GUIDE.md`, Phase 01.
- Version baseline verified against official sources (plan §4). No milestone, RC, or snapshot is planned.
- `git ls-files --eol`: all 10 files stored and checked out with LF.
- `git check-ignore`: secrets, build output, dependencies, logs, and backups are ignored. `.env.example` is not.
- `git check-attr`: `.sh` and `mvnw` get LF, `.cmd` and `.ps1` get CRLF, images are binary.
- FILE_GUIDE coverage: 10 tracked files, 10 matching rows and headers, 0 problems.
- Documentation links: 12 relative links and anchors, 0 broken.
- Secret scan of the staged diff: 0 matches.

Not run in Phase 01, because nothing exists yet to run: builds, tests, containers, browser checks, benchmarks,
pushes, deployments.

**GitHub setup checks (2026-10-20):** `gh auth status` showed account `RishitheB0T` with its token in the system
keyring. After `git push -u origin main`, the local hash, `git ls-remote`, and the GitHub API all returned
`5be2fc39…`. `gh repo view` showed visibility `PUBLIC` and default branch `main`.

<!-- BUILD FLOW 01.10.06 — Checks run in Phase 02 -->
## Checks run in Phase 02

All ran on 2026-10-20. Full details, including two problems found and fixed, are in `BUILD_GUIDE.md`, Phase 02.
- Maven 3.10.0 zip: SHA-512 matched Apache's and Maven Central's published values, and the wrapper accepted our SHA-256 pin.
- `.\mvnw.cmd clean verify`: 6 tests, 0 failures; runnable jar built. (The first run had 2 failures caused by
  Spring Boot 4's probe groups; the tests were corrected to check the intent.)
- Dev run (`spring-boot:run`, `dev` profile): port 8081, health UP with component details, `/actuator/env` 404.
- Prod run (`java -jar`, `prod` profile): health UP without details, readiness UP, `/actuator/env` 404, no banner.
  The first prod jar lacked its YAML files; a `clean verify` fixed it (likely cause: the editor's background build).
- Git: `backend/mvnw` is executable (`100755`); `mvnw.cmd` has CRLF in the working tree.

**Carry-forward items**
- Tests print a JDK warning about a dynamically loaded agent (Mockito/Byte Buddy). Configure the agent properly
  when mocks are first used (Phase 05).
- The VS Code Java extension may build into `backend/target/` in the background. Use `clean` for jars you run or
  ship; Phase 14 builds images in clean containers and checks the artifact itself.

<!-- BUILD FLOW 01.10.07 — Checks run in Phase 03 -->
## Checks run in Phase 03

All ran on 2026-10-20. Full details are in `BUILD_GUIDE.md`, Phase 03.
- Port block 24xxx: not in use, outside Windows' reserved ranges, not used by the other projects on the machine.
- `.\scripts\dev-services.ps1 up`: MySQL 8.4.11 and Redis 8.10.2 healthy; `infra/.env` generated and git-ignored.
- MySQL: `utf8mb4`, app user limited to the `catalog` database; data survived `down` + `up`.
- Redis: `PONG`; no persistence (`save` empty, `appendonly no`), confirmed by restarting the same container; 128 MB `allkeys-lru`.
- Network: 24081, 24306, and 24379 listen on `127.0.0.1` only; unreachable through the LAN address.
- Backend: `clean verify` 6/6 tests; dev run on `127.0.0.1:24081`; DEBUG logging for `com.example.catalog` active.

<!-- BUILD FLOW 01.10.08 — Checks run in Phase 04 -->
## Checks run in Phase 04

All ran on 2026-10-20. Full details, including the first run's six failures, are in `BUILD_GUIDE.md`, Phase 04.
- `.\mvnw.cmd clean verify`: 21 tests, 0 failures, against one shared `mysql:8.4.11` test container.
- Dev profile against the Compose MySQL: Flyway applied V1, Hibernate validated the schema, 24 demo products loaded,
  health `db` UP. A second start migrated nothing and loaded nothing.
- Database inspection: the named constraints, only the primary-key and SKU indexes, UTC timestamps.
- Packaged jar with `prod` and `SPRING_DATASOURCE_*` environment variables: starts, no details, no demo data.

**Carry-forward items**
- The JDK warning about a dynamically loaded agent (Mockito): resolved in Phase 05 (decision D-030).
- Plain `JdbcTemplate` reports MySQL CHECK violations (error 3819) as `UncategorizedSQLException`; repository (JPA)
  calls get `DataIntegrityViolationException`. Map the latter in Phase 05; remember the former if raw JDBC writes appear.
- Maven prints two HTTP 401 warnings for repositories declared in Flyway's parent POM; all jars come from Maven Central.

<!-- BUILD FLOW 01.10.09 — Checks run in Phase 05 -->
## Checks run in Phase 05

All ran on 2026-10-21. Full details are in `BUILD_GUIDE.md`, Phase 05.
- `.\mvnw.cmd clean verify`: 43 tests, 0 failures (14 controller, 8 service, 13 repository, 6 health, 2 demo data).
- Manual CRUD against the dev server with `curl.exe`: every status code and Problem Details type behaved as designed.
- The packaged jar with `prod`: reads 200, and POST, PUT, and DELETE each 403 `writes-disabled`.
- The README's `Invoke-RestMethod` examples ran verbatim.

**Carry-forward items**
- The test JVM prints one harmless Class Data Sharing notice caused by the Mockito agent (D-030).
- Windows PowerShell 5.1's `Invoke-RestMethod` does not show Problem Details bodies on errors; use `curl.exe`.

<!-- BUILD FLOW 01.10.10 — Checks run in Phase 06 -->
## Checks run in Phase 06

All ran on 2026-10-21. Full details are in `BUILD_GUIDE.md`, Phase 06.
- `.\mvnw.cmd clean verify`: 64 tests, 0 failures (22 controller, 13 search, 13 repository, 8 service, 6 health,
  2 demo data).
- 19 manual searches with `curl.exe` against the dev server with SQL logging: filters, sorts, ties, paging, and every
  kind of invalid parameter behaved as designed. Search text was always a bound value, and no SQL ran for invalid
  requests.
- Queries per request, counted in the SQL log: a full first page 2 (page + `COUNT`), a partial last page 1, a page past
  the end 2.
- The README's search example ran verbatim.

**Carry-forward items**
- No search indexes yet, on purpose: Phase 08 measures this baseline with `EXPLAIN ANALYZE` first.
- A repeated query parameter uses its first value, and conversion errors are reported before (instead of together
  with) rule violations (D-034). Phase 07's HTTP tests can pin these down. (Done in Phase 07.)

<!-- BUILD FLOW 01.10.11 — Checks run in Phase 07 -->
## Checks run in Phase 07

All ran on 2026-10-22. Full details are in `BUILD_GUIDE.md`, Phase 07.
- `.\mvnw.cmd clean verify`: 85 tests, 0 failures (22 controller, 15 API, 13 search, 13 repository, 8 service,
  6 search API, 6 health, 2 demo data). 5 real servers and 10 connection pools for the whole run.
- Race tests: ten simultaneous updates of one version end as one 200 and nine 409s; ten simultaneous creates with one
  SKU end as one 201, nine 409s, and one row.
- Every request in `backend/api-examples.http`, sent with `curl.exe` against the dev server, answered as its title says.
- A manual race with `curl.exe --parallel` (three rounds of ten PUTs): one winner each time; all losers were stopped
  by Hibernate's `WHERE version = ?`, because all ten requests had read version 0 before the winner committed.

**Carry-forward items**
- Each distinct test configuration starts another application with its own connection pool: 10 pools of up to 10
  connections against the one test MySQL (default limit 151). Reuse existing configurations where possible, or give
  tests smaller pools, before adding many more (D-036).
- Every 405 is logged by Spring as a warning ("Request method … is not supported"). Harmless now; worth filtering
  when real traffic arrives (Phase 15).

<!-- BUILD FLOW 01.10.12 — Checks run in Phase 08 -->
## Checks run in Phase 08

All ran on 2026-10-22. Full details are in `BUILD_GUIDE.md`, Phase 08, and the evidence in `docs/QUERY_OPTIMIZATION.md`.
- `.\mvnw.cmd clean verify`: 91 tests, 0 failures, both before V2 existed and with V2 applied.
- Benchmark database `catalog_bench`: 100,000 generated products (fingerprint pinned by a test), loaded in 3.8–5.6 s.
- The SQL of 8 typical requests captured from MySQL's query log: 12 distinct statements.
- `EXPLAIN ANALYZE` before and after V2: page queries 73–483× faster, the category count 5.8×, other counts and the
  deep page unchanged, the no-match name search 44.5 → 193 ms; every result identical (SHA-256 fingerprints).
- A full reproduction from a fresh database (`--spring.flyway.target=1`, then V2) gave identical fingerprints.

**Carry-forward items**
- Known slow cases, accepted for now (D-041): counts, the deepest pages, and name searches that match nothing.
  Phase 11 measures them end to end before anything is changed.
- The test run now holds 11 connection pools (up to 110 connections against MySQL's default 151). Phases 09–10 add
  Redis test configurations: give tests smaller pools or reuse configurations first (D-036).
- `catalog_bench` stays in the MySQL volume (100,000 products, schema V2) for the Phase 11 benchmarks;
  `.\scripts\bench-db.ps1 drop -Force` removes it.

<!-- BUILD FLOW 01.10.13 — Checks run in Phase 09 -->
## Checks run in Phase 09

All ran on 2026-10-23. Full details are in `BUILD_GUIDE.md`, Phase 09, and `docs/CACHING.md`.
- `.\mvnw.cmd clean verify`: 100 tests, 0 failures (the first run failed to start any test application until the
  Redis test container's connection was named).
- By hand with the cache on: one key with TTL 59 s holding the API's JSON; hit 1, miss 1, error 0; no key for a 404;
  health UP without Redis.

**Carry-forward items**
- The cache is off by default (D-044): writes do not evict yet. Phase 10 turns it on after adding eviction.
- Lettuce's default command timeout is 60 seconds; Phase 10 sets short timeouts and tests a Redis outage.
- The `bench` profile has no Redis settings yet; Phase 11 adds them for the cache on/off benchmark. (Added in Phase 10.)

<!-- BUILD FLOW 01.10.14 — Checks run in Phase 10 -->
## Checks run in Phase 10

All ran on 2026-10-29. Full details are in `BUILD_GUIDE.md`, Phase 10, and `docs/CACHING.md`.
- `.\mvnw.cmd clean verify`: 107 tests, 0 failures, with the cache on in every test application.
- The outage test paused Redis: cache steps gave up after about 200–230 ms; reads and a write still succeeded.
- On the dev stack: a PUT removed the key at once; with Redis stopped, reads took about 430 ms with correct data and
  an update succeeded while its eviction failed (counted); after a restart, hits returned.
- The README's production-style run with the new Redis variables worked as written.

**Carry-forward items**
- A stopped Redis costs two 200 ms timeouts per product read; rejecting commands while disconnected or a circuit
  breaker would remove that (D-047). Phase 11 can show whether it matters.
- The stale-read race and stampedes remain documented limits (D-046, `docs/CACHING.md`).
- Docker Desktop is installed per user; start it from its own install location if `docker info` fails.

<!-- BUILD FLOW 01.10.15 — Checks run in Phase 11 -->
## Checks run in Phase 11

All ran on 2026-10-29 and 2026-10-30. Full details are in `BUILD_GUIDE.md`, Phase 11, and the results in
`docs/PERFORMANCE.md`.
- `.\scripts\run-benchmarks.ps1`, the published run (2026-10-30, 18:32): 15 trials, each clean on its first attempt,
  with no failed request. Product detail served 1,016 requests per second with the cache off, 579 cold, and 3,310
  warm. The search mix served 44.8 requests per second without the indexes and 144.1 with them.
- Four earlier full runs were discarded, each for a reason the script now checks: Modern Standby, connections stalling
  in Docker Desktop's forwarding (twice), and cold passes that weren't cold. None of their numbers is published.
- Cold passes: 0 hits and 1,000 misses each. Warm trials: a 99.62% hit ratio; Redis evicted no keys.
- Little's law matched every measured rate within 1%.
- The script's rules were tested on eight synthetic k6 results, and its key deletion on the dev Redis.
- Afterwards: no backend process or k6 container left; ports 24081–24083 free.

**Carry-forward items**
- Counts now dominate search time under load (`docs/PERFORMANCE.md`); D-041's options are the next lever if search
  speed matters.
- Not measured under load: a stopped Redis (the two 200 ms timeouts per read from Phase 10), writes, the rare-word
  search, and the deepest page.
- Windows PowerShell 5.1 on this machine puts a byte-order mark in front of text piped into native programs. The
  `mysql` client ignores it (`bench-db.ps1` and `measure-queries.ps1` work), and `redis-cli` does not. Pass arguments
  instead of piping to new tools.
- Benchmarks need mains power and an open lid. The script keeps the computer awake, but a closed lid still sends it
  into standby (and the run is then discarded).

<!-- BUILD FLOW 01.10.16 — Checks run in Phase 12 -->
## Checks run in Phase 12

All ran on 2026-10-30. Full details are in `BUILD_GUIDE.md`, Phase 12.
- Version re-check: Node 26 LTS since 2026-10-28; Vite 8.3.5 (a patch); the template lints with oxlint (D-053).
- `npm install` (100 packages, 0 vulnerabilities reported); `npm run lint` clean; `npm test` 17 passed; `npm run
  build` succeeded.
- Backend `.\mvnw.cmd clean verify`: 108 tests, 0 failures (one new test for the widened `imageUrl` rule).
- Real stack (dev services reset, the backend with `dev`, `npm run dev`): 24 demo products with seven photo paths;
  the page, `/api` through the Vite proxy, and a photo all answered 200.
- Headless browser at 375, 768, and 1440 pixels:
  - layout: no sideways scrolling; 2 / 3 / 3 columns; the sidebar or the drawer as designed;
  - photos: every photo on page 1 loaded;
  - behaviour: one request per typed search; Back restored the search; the page reset to 1; reversed prices were
    explained and not sent; readable API errors; the empty states; the drawer as a modal with Escape and focus
    return; the skip link first.

**Carry-forward items**
- Only seven of 24 demo products have photos, and they don't share a style. Better photos (Openverse when it
  responds, or photos the developer provides) can replace placeholders under the rules in `docs/IMAGE_CREDITS.md`.
- Routing is decided in Phase 13, when the detail and admin views arrive (D-050).
- The development machine runs Node 26.8.1. Updating to the 26 LTS release (26.13.0) is recommended before Phase 14
  builds images with it.
- The local `catalog_bench` database was deleted by the dev reset; `scripts/run-benchmarks.ps1` recreates it.

<!-- BUILD FLOW 01.10.03 — Environment -->
## Environment (inspected 2026-10-20)

| Item | Found |
|---|---|
| Operating system | Windows 11 Home, 64-bit (build 26200); Windows PowerShell 5.1 |
| Hardware | 4 cores / 8 threads, 15.8 GB RAM. About 1 GB was free during inspection, with browsers holding much of it. Docker's Linux VM: 8 CPUs, about 7.6 GiB |
| Git | 2.55.0, with Git Credential Manager 2.9.1 configured (GitHub account `RishitheB0T`) and a Git identity set |
| GitHub CLI | 2.102.0, installed with winget on 2026-10-20 and signed in as `RishitheB0T` |
| JDK | Microsoft Build of OpenJDK 21.0.12.1; `JAVA_HOME` set |
| Docker | Engine 29.6.2 (WSL 2 backend) and Compose v5.3.1; the engine was running |
| Node.js / npm | 26.8.1 / 11.19.0 (Node 26 became LTS on 2026-10-28; the LTS release is 26.13.0, re-checked in Phase 12) |
| Maven | 3.10.0 through the wrapper (downloaded to the user's `.m2` folder on 2026-10-20; checksum-verified) |
| Ports | This project uses the 24xxx block on `127.0.0.1` (README "Ports"). At inspection, 8080, 5432, and 9090 were in use by other programs |
| Local containers | `product-catalog-dev-mysql-1` and `product-catalog-dev-redis-1`; together about 430 MiB of memory while running |
| Test containers | One shared `mysql:8.4.11` container per test run (about 400 MB) plus Testcontainers' cleanup container; Docker must be running |
| k6 | Not installed; runs from the pinned `grafana/k6:2.3.0` image (Phase 11) |
| Power | Modern Standby (S0 low-power idle): the system sleeps when the display turns off. Benchmarks keep it awake and need mains power |
| Not installed | AWS CLI (decided in Phase 15) |

<!-- BUILD FLOW 01.10.04 — Blockers and pending items -->
## Blockers and pending items

| Item | Needed by | Status |
|---|---|---|
| GitHub repository and sign-in | — | Done on 2026-10-20: public repository created with the GitHub CLI; pushes verified |
| `workflow` token scope for pushing CI files | Phase 14 | Pending. The current tokens are not known to include `workflow`, which GitHub requires for pushing files under `.github/workflows/`. Plan: `gh auth refresh -s workflow` when needed |
| AWS account access and approval of instance, region, and cost | Phase 15 | Pending |
| Anything blocking Phase 13 | — | None |

<!-- BUILD FLOW 01.10.05 — Next action -->
## Next action

Next: Phase 13 (plan §14, "Detail, admin, accessibility, polish").
1. Record Phase 12's commit hash in the commits table above.
2. Decide routing for the detail and admin views (React Router or a small hand-written router; D-050).
3. The product detail view: image, description, price, stock, and SKU, from `GET /api/v1/products/{id}`.
4. The local admin: create, edit, and delete with validation messages from the API, version conflicts (409),
   delete confirmation in an accessible dialog, and success feedback. It respects the server's write switch, and the
   server still enforces it.
5. Accessibility: keyboard paths, visible focus, labeled inputs, dialogs, contrast, reduced motion.
6. Checks: frontend tests, and browser checks of CRUD, keyboard paths, and mobile layouts.
7. Commit, push, and verify on GitHub.
