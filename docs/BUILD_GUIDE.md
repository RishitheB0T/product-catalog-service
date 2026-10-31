<!--
BUILD FLOW 01.05 — docs/BUILD_GUIDE.md
First introduced: Phase 01
Purpose: The project in construction order. Read it top to bottom to rebuild the project in your head.
Depends on: docs/PROJECT_PLAN.md (phase definitions) and docs/FILE_GUIDE.md (file details).
Read by: Anyone studying the build, including for interview preparation.
Runtime role: None. Documentation only.
-->

# Build Guide — the project in construction order

Each phase below answers the same questions: what we had before, what we add, why, the files in order, the
relevant syntax, the commands, the expected behavior, the actual checks, and how the pieces connect. When a later
phase changes an existing file, the change is listed under that later phase, and the file keeps its original number.

<!-- BUILD FLOW 01.05.01 — Two numbering systems -->
## Two numbering systems (don't mix them up)

| | BUILD FLOW number | Runtime step ID |
|---|---|---|
| Looks like | `05.03`, `05.03.02` | `SEARCH-03`, `READ-04`, `WRITE-06` |
| Answers | When was this file (or section) first written? | In what order does code run during one request? |
| Lives in | The top-of-file comment and section comments | `docs/REQUEST_FLOWS.md`, and code comments such as `// Runtime: READ-04` |
| Changes? | Never. A file keeps its number | Yes, when the flow's behavior changes |

Example (planned): `ProductService` will be written in Phase 05, so its number will start with `05.`. During a
product-detail request it runs at step `READ-03`, *after* the controller (`READ-02`). Phase 09 then adds the cache
steps `READ-04` and `READ-07` around it, but the file keeps its `05.` number.

<!-- BUILD FLOW 01.05.02 — Phase 01 -->
## Phase 01 — Workspace checks, architecture, and the phased build plan

**What we had before.** An empty folder. It was not a Git repository and neither was its parent folder, so a new
repository here could not end up nested inside another one.

**What we add.** No application code yet. We add the foundations every later phase relies on: the canonical plan, Git rules for ignored files and line endings, a README that tells the
truth about status, and the living guides.

**Why.** A 16-phase build spans many sittings. Without a written plan, rules, and progress log, it is easy to lose track
of what was decided. The ignore and line-ending rules must exist *before* the first commit, so that
no secret or wrongly converted file ever enters the history.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 01.01 | `docs/PROJECT_PLAN.md` | Canonical plan: agreement, requirements, versions, 16 phases |
| 01.02 | `.gitignore` | What Git must never track (secrets, build output, dependencies) |
| 01.03 | `.gitattributes` | Line-ending and binary-file rules |
| 01.04 | `README.md` | Front page with honest status |
| 01.05 | `docs/BUILD_GUIDE.md` | This guide |
| 01.06 | `docs/FILE_GUIDE.md` | Every tracked file explained |
| 01.07 | `docs/REQUEST_FLOWS.md` | Planned runtime flows and step IDs |
| 01.08 | `docs/DECISIONS.md` | Decisions and trade-offs |
| 01.09 | `docs/LEARNING_LOG.md` | Concepts and exercises |
| 01.10 | `docs/PROGRESS.md` | Current state and the next action |

**Relevant syntax**
- *Markdown*: `#` headings, `| a | b |` tables, `[text](path)` links, and fenced code blocks. `<!-- … -->` is an
  HTML comment: it is visible in the source and hidden when rendered. Markdown files keep their BUILD FLOW header there.
- *.gitignore*: one pattern per line. `target/` matches a directory with that name at any depth, `*.pem` is a
  wildcard, `!.env.example` re-includes something an earlier pattern excluded, and `#` starts a comment.
- *.gitattributes*: `pattern attribute=value`. `text=auto` lets Git decide whether a file is text, `eol=lf` checks
  text files out with LF line endings, and `binary` turns off line-ending conversion and text diffs.

**Commands we ran** (PowerShell)
- Inspection: `Get-ChildItem -Force`; `git rev-parse --show-toplevel` (here and in the parent folder);
  `git config --global user.name`; `java -version`; `node --version`; `npm --version`; `docker version`;
  `docker compose version`; `Get-Command <tool>` for each tool.
- Version checks: HTTP requests to the official sources listed in `docs/PROJECT_PLAN.md` §4.
- `git init -b main`: create the repository with `main` as its first branch.
- Checks: `git check-ignore -v`, `git check-attr`, `git ls-files --eol`, a FILE_GUIDE coverage check, a link
  check, and a secret scan of the staged diff.
- `git add <explicit paths>`, then `git commit`.

**Expected behavior.** No program runs yet. The repository has one commit. A future `.env` file, `target/` folder,
or `node_modules/` folder is ignored. Shell scripts are always checked out with LF endings, and `.cmd`/`.ps1` files with CRLF.

**Actual checks** (run on 2026-10-20 against the staged files, before the commit)

| Check | How | Result |
|---|---|---|
| Version baseline | Official sources listed in plan §4 | Every planned version exists as a stable release. Excluded as not stable: Spring Boot 4.2.0-M2 (milestone) and Maven 4.0.0-rc-7 (release candidate) |
| What is staged | `git diff --cached --stat` | Exactly the 10 Phase 01 files |
| Line endings | `git ls-files --eol` | All 10 files: `i/lf w/lf attr/text=auto eol=lf` (LF in Git's index and on disk) |
| Ignore rules | `git check-ignore -v --no-index <path>` | Ignored: `.env`, `infra/.env`, `.env.production`, `backend/target/…`, `frontend/node_modules/…`, `frontend/dist/…`, `keys/*.pem`, `logs/app.log`, `backups/…`. Not ignored: `.env.example` (re-included by `!.env.example`; the plain command exits with 1) and `docs/PROJECT_PLAN.md` |
| Line-ending rules | `git check-attr text eol -- <path>` | `.sh` and `mvnw`: `eol: lf`. `mvnw.cmd` and `.ps1`: `eol: crlf`. `.webp`: `text: unset`, `diff: unset` (binary) |
| File guide coverage | Script: every `git ls-files` path has a FILE_GUIDE row and a BUILD FLOW header, and the numbers match | 10 files, 10 rows, 0 problems |
| Documentation links | Script: relative links and `#anchors` in tracked Markdown, ignoring code | 12 links, 0 broken |
| Secret scan | Script: staged added lines checked for cloud keys, tokens, private keys, assigned passwords, email addresses, and Windows user paths | 0 matches |

Two lessons from running these checks:
- My first ignore report listed `.env.example` as ignored. That was wrong. With `-v`, Git also prints matches of
  `!` (re-include) rules. Without `-v`, the exit code is the reliable answer: 0 means ignored, 1 means not ignored.
- The first link check flagged the syntax example `[text](path)` in this guide. It is inside backticks, so it is
  code, not a link. The checker now skips code spans and code blocks.

**How the pieces connect**

```text
README.md ──links──► docs/PROGRESS.md ──"next action"──► docs/PROJECT_PLAN.md §14 (the phase to build)
docs/BUILD_GUIDE.md  ◄── updated every phase ──►  docs/FILE_GUIDE.md (one row per tracked file)
.gitignore + .gitattributes ──► decide what enters every future commit, and with which line endings
```

<!-- BUILD FLOW 01.05.03 — Between Phases 01 and 02: connecting GitHub -->
## Between Phases 01 and 02 — Connecting GitHub

**What we had before.** One commit (`5be2fc3`) that existed only on this laptop.

**What we add.** A public GitHub repository, the `origin` remote, and one rule: every commit is pushed and verified right after it is made. No new files: the rule was added to
`PROJECT_PLAN.md` §12.

**Why.** The history should be visible on GitHub as it grows, and CI (Phase 14) runs there.

**Commands we ran** (PowerShell)

| Command | What it did |
|---|---|
| `winget install --id GitHub.cli -e --source winget` | Installed GitHub CLI 2.102.0. winget verified the installer's hash before running it |
| `gh auth login --hostname github.com --git-protocol https --web` | Browser "device flow" sign-in: a one-time code was approved at github.com/login/device, and the token went into the Windows credential store |
| `gh repo create product-catalog-service --public --source . --remote origin` | Created the empty public repository and added it as the remote named `origin` |
| `git push -u origin main` | Uploaded `5be2fc3`. `-u` made local `main` track `origin/main` |
| `git ls-remote origin refs/heads/main` and `gh api repos/…/commits/main` | Asked GitHub which commit its `main` points to |

**Actual checks**
- `gh auth status`: signed in as `RishitheB0T`, token in the system keyring, scopes `repo`, `read:org`, `gist`.
- The local `main`, `git ls-remote`, and the GitHub API all returned `5be2fc39…`, so GitHub has exactly our commit.
- `gh repo view`: visibility `PUBLIC`, default branch `main`.
- One hiccup: right after the successful push, the first `git ls-remote` failed with "Could not resolve host:
  github.com" (a momentary DNS failure). Retrying it, and cross-checking through the API, gave the result above.

**How the pieces connect**

```text
local commit ──git push──► origin (GitHub) ──► visible history; from Phase 14, CI runs on every push
```

<!-- BUILD FLOW 01.05.04 — Phase 02 -->
## Phase 02 — Spring Boot skeleton, profiles, and a health check

**What we had before.** Documentation only, now on GitHub. No code.

**What we add.** A Maven project in `backend/`: Spring Boot 4.1.1 on Java 21 with only Web MVC, Validation, and
Actuator; a Maven Wrapper pinned to Maven 3.10.0 with a verified checksum; a main class; three configuration files
(base, `dev`, `prod`); and tests that start the real application and call it over HTTP.

**Why.** It is the smallest runnable backend. It settles the build tool, how configuration is layered, how the app
reports health (Docker health checks in Phase 14 will rely on it), and how we test, all before any business code.
Database and Redis libraries are left out on purpose (decision D-004): with them on the classpath, the app would
refuse to start without MySQL.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 02.01 | `backend/pom.xml` | Build description: parent, Java version, starters, packaging |
| 02.02 | `backend/mvnw` | Generated wrapper script (Linux) |
| 02.03 | `backend/mvnw.cmd` | Generated wrapper script (Windows) |
| 02.04 | `backend/.mvn/wrapper/maven-wrapper.properties` | Generated wrapper settings plus our SHA-256 pin |
| 02.05 | `backend/src/main/java/com/example/catalog/CatalogApplication.java` | Entry point |
| 02.06 | `backend/src/main/resources/application.yml` | Base configuration (safe defaults) |
| 02.07 | `backend/src/main/resources/application-dev.yml` | Dev profile |
| 02.08 | `backend/src/main/resources/application-prod.yml` | Prod profile |
| 02.09 | `backend/src/test/java/com/example/catalog/HealthEndpointTest.java` | Real-HTTP tests under three configurations |

Modified: `.gitignore` (01.02) gained section 01.02.06 for Java editor metadata and a wrapper jar.
Generated but not kept: `HELP.md`, `backend/.gitignore`, `backend/.gitattributes`, `application.properties`,
`CatalogApplicationTests` (reasons in `FILE_GUIDE.md`).

**Relevant syntax**
- *XML (`pom.xml`)*: nested elements (`<parent>…</parent>`). A comment `<!-- … -->` may not contain two hyphens in a
  row, which is why the comments never spell out command-line flags.
- *Maven coordinates*: `groupId:artifactId:version` identifies every library. `<scope>test</scope>` limits a
  dependency to tests.
- *Java*: `package` (the folder-like namespace), `import`, annotations (`@SpringBootApplication`, `@Test`),
  `public static void main(String[] args)` (the JVM's entry point), `static final` (one shared constant),
  `throws` (a method may fail with these checked exceptions), generics (`HttpResponse<String>` is a response whose
  body is a `String`), and inner classes marked `@Nested`.
- *YAML*: indentation creates nesting, and text that YAML treats specially (`off`, `on`, `yes`, `no`) must be quoted.
- *PowerShell*: quote `-D` arguments containing dots, for example `"-Dspring-boot.run.profiles=dev"`.

**Commands we ran** (PowerShell; Maven commands run inside `backend/`)

| Command | Purpose |
|---|---|
| Download from start.spring.io (Boot 4.1.1, Java 21, Maven, `web`, `validation`, `actuator`) | Generate the skeleton into a scratch folder, then copy selected files |
| Download Maven 3.10.0, compare its SHA-512 with Apache's and Maven Central's published values, then compute its SHA-256 | Produce a trustworthy `distributionSha256Sum` |
| `git add --chmod=+x -- backend/mvnw` | Record the executable bit Linux needs |
| `.\mvnw.cmd --version` | First wrapper run: downloads and verifies Maven 3.10.0 |
| `.\mvnw.cmd -B -ntp verify` and later `clean verify` | Compile, test, and package (`-B`: batch mode, `-ntp`: no download progress) |
| `.\mvnw.cmd help:effective-pom` | Show the merged configuration Maven really uses (while investigating) |
| `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"` | Run locally with the dev profile |
| `java -jar target\catalog-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --server.port=8082` | Run the packaged jar the way a server would (8082 only because 8080 is occupied on this laptop) |
| `curl.exe http://localhost:<port>/actuator/health` (and `/readiness`, `/env`) | Call the running app |

**Expected behavior.** All tests pass. The app starts in a few seconds. `/actuator/health` returns 200 with
`"status":"UP"`. Component details appear only with `dev`. `/actuator/env` returns 404 everywhere. The prod jar
prints no banner.

**Actual checks** (2026-10-20)

| Check | Result |
|---|---|
| Wrapper first run | Maven 3.10.0 downloaded, accepted by the SHA-256 pin, running on JDK 21.0.12.1 |
| First `verify` | **2 of 5 tests failed.** Spring Boot 4 enables liveness/readiness probe groups automatically, so the body is `{"groups":["liveness","readiness"],"status":"UP"}`, not `{"status":"UP"}`. Confirmed in the Actuator docs. Tests changed to check the intent (UP, no component details) and a probe test was added (decision D-013) |
| Second `verify` | 6 tests, 0 failures. Jar built (22.5 MB) |
| Dev run | Profile `dev`, Tomcat on 8081, started in 2.7 s. Health 200 with components (diskSpace, ping, ssl, livenessState, readinessState); `/actuator/env` 404 |
| First prod-jar run | **Problem found:** banner printed and no `[catalog]` name in the logs. The jar contained no `application*.yml` files, although the build log showed them copied to `target/classes` and the dev test (which needs them) had passed |
| Investigation | `.jar.original` (Maven's own jar) also lacked them; the effective POM had no exclusions; the VS Code Java language server was running. A `clean verify` produced a jar with all 3 YAML files. Most likely cause: the editor's background build rewrote `target/` while Maven was packaging (not proven) |
| Second prod-jar run (after `clean verify`) | No banner, `[catalog]` in every log line, profile `prod`, health UP without details, `/actuator/env` 404 |
| Warnings | During tests only, the JDK warns about a "dynamically loaded agent" (Mockito's helper, Byte Buddy). Harmless today; to be configured properly when mocks arrive in Phase 05 |
| Git | `backend/mvnw` stored as executable (`100755`); `mvnw.cmd` checked out with CRLF, stored with LF |

Lessons: a test suite can pass while the shipped artifact is wrong, so check the artifact itself. Build what you
ship from a clean state. Phase 14 builds images inside clean containers and will smoke-test production-only behavior.

**How the pieces connect**

```text
mvnw.cmd ──reads──► maven-wrapper.properties ──downloads + verifies──► Maven 3.10.0
Maven ──reads──► pom.xml ──inherits──► spring-boot-starter-parent 4.1.1 (tested library versions)
  └─ compile ─► test (HealthEndpointTest starts the app 3 times) ─► package (runnable jar)
java -jar / spring-boot:run ─► CatalogApplication.main ─► SpringApplication.run
  ─reads─► application.yml + application-<profile>.yml ─► Tomcat + Actuator ─► /actuator/health
```

<!-- BUILD FLOW 01.05.05 — Phase 03 -->
## Phase 03 — Local MySQL and Redis with Docker Compose

**What we had before.** A backend that starts on its own but has nowhere to store data. Its dev port was 8081.

**What we add.** MySQL 8.4.11 and Redis 8.10.2 as Docker containers defined in a Compose file, a template for their
private settings, a PowerShell helper to manage them, and a project-specific block of host ports (24xxx) bound to
loopback only. The backend's dev server moves into the same block: `127.0.0.1:24081`.

**Why.** Phase 04 needs a real MySQL for the schema, and Phase 09 a real Redis. Containers give the same pinned
versions with one command and can be reset in seconds. The port block prevents clashes with the other projects on
the same machine.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 03.01 | `infra/compose.dev.yaml` | The MySQL and Redis services |
| 03.02 | `infra/.env.example` | Template for the private `infra/.env` |
| 03.03 | `scripts/dev-services.ps1` | `up` / `status` / `logs` / `down` / `reset -Force`; random passwords on the first `up` |

Modified: `backend/src/main/resources/application-dev.yml` (02.07) now sets `server.address: 127.0.0.1` and
`server.port: 24081` (was 8081), and writes the logging key in bracket notation.

**Relevant syntax**
- *Compose YAML*: `services` has one entry per container, with `image`, `environment`, `ports`, `volumes`,
  `healthcheck`, and `command`. `"127.0.0.1:24306:3306"` means host address : host port : container port.
  `${NAME:-default}` and `${NAME:?error}` substitute values from `.env`; `$$` hands a literal `$` to the container's shell.
- *Health checks*: `test` runs inside the container every `interval`; `retries` failures in a row mark it unhealthy;
  failures during `start_period` do not count.
- *PowerShell*: `param()` with `[ValidateSet(...)]` and `[switch]`, `$PSScriptRoot`, the `switch` statement,
  splatting an array into a native command (`docker @composeArgs`), and `try`/`finally`.
- *Spring Boot*: bracket notation for map keys that contain dots: `"[com.example.catalog]": debug`.

**Commands we ran** (PowerShell, repository root)

| Command | Purpose |
|---|---|
| `netsh interface ipv4 show excludedportrange protocol=tcp` and `Get-NetTCPConnection -State Listen` | Find reserved and busy ports before choosing the block |
| Docker Hub tag lookups for `mysql:8.4.*` and `redis:8.10.*` | Re-verify the newest patch tags |
| `.\scripts\dev-services.ps1 up` | Create `infra/.env`, pull the images, start both containers, wait until healthy |
| `docker compose … exec -T mysql sh -c '…mysql…'` with the SQL sent on standard input | Query MySQL as the app user without exposing the password |
| `docker compose … exec -T redis redis-cli PING` and `CONFIG GET` | Check Redis and its cache settings |
| `.\scripts\dev-services.ps1 down`, then `up` | Prove that the MySQL data volume survives |
| `docker compose … restart redis` | Prove that Redis keeps nothing across a restart |
| `.\mvnw.cmd clean verify` and `spring-boot:run` with `dev` | Re-test the backend and check its new address and port |

**Expected behavior.** Both containers healthy; their ports reachable from this computer only; MySQL data survives
`down`/`up`; Redis starts empty after a restart; the backend answers on `127.0.0.1:24081` only.

**Actual checks** (2026-10-20)

| Check | Result |
|---|---|
| Port block | The five candidate ports were not in use, not inside Windows' reserved ranges, and not mentioned in the other projects' 481 configuration files. At that time 8080, 5432, and 9090 were in use by other programs |
| Images | `mysql:8.4.11` and `redis:8.10.2-alpine` were still the newest patches |
| `dev-services.ps1 up` | Both containers healthy about 51 s after the first start, image downloads included; `infra/.env` created and git-ignored |
| MySQL | Version 8.4.11, user `catalog@%`, database `catalog`, `utf8mb4` / `utf8mb4_0900_ai_ci`, 128 MB buffer pool. The user's grants cover only the `catalog` database |
| Redis | `PONG`, version 8.10.2, `save` empty, `appendonly no`, `maxmemory` 134217728 bytes (128 MB), `allkeys-lru` |
| Network | 24306, 24379, and 24081 listen on `127.0.0.1` only; connecting through the laptop's LAN address fails |
| Persistence | A MySQL row survived `down` + `up` (containers removed, volume kept). A Redis key was gone after restarting the **same** container, and its shutdown log shows no snapshot being saved |
| Resources | MySQL about 426 MiB of memory, Redis about 6 MiB |
| Backend | `clean verify`: 6 tests, 0 failures. Dev run on `127.0.0.1:24081`, health 200, and the DEBUG line from `com.example.catalog` present, so the bracket notation works |

Problems met on the way:
- The first MySQL check failed with a SQL syntax error. Windows PowerShell 5.1 drops double quotes nested inside
  arguments passed to external programs. Sending the SQL through standard input fixed it.
- The first Redis "no persistence" check used `down` and `up`, which creates a brand-new container and therefore
  proves nothing about persistence. Restarting the same container is the real test, and it passed.

**How the pieces connect**

```text
dev-services.ps1 up ──creates──► infra/.env (random passwords, git-ignored)
   └─► docker compose (project "product-catalog-dev") ──reads──► compose.dev.yaml + infra/.env
         ├─ mysql  (named volume product-catalog-dev_mysql-data) ◄── 127.0.0.1:24306
         └─ redis  (memory only, 128 MB, LRU eviction)          ◄── 127.0.0.1:24379
backend (dev profile) on 127.0.0.1:24081 ──► uses MySQL from Phase 04 and Redis from Phase 09
```

<!-- BUILD FLOW 01.05.06 — Phase 04 -->
## Phase 04 — Schema, entity, repository, and demo data

**What we had before.** A backend that started without a database, and MySQL and Redis containers that nothing used yet.

**What we add.** The persistence layer: a Flyway migration that creates the `product` table with rules the database
enforces itself, the `Category` enum, the `Product` entity, `ProductRepository`, a dev-only demo catalog, and tests
that run against a real MySQL 8.4.11 in Docker. From now on the application connects to MySQL when it starts.

**Why.** Everything later (CRUD, search, caching) reads and writes this table. Getting the schema, its constraints,
time handling, and optimistic locking right, and proving them, keeps the later phases simpler.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 04.01 | `backend/src/main/resources/db/migration/V1__create_product_table.sql` | The table and its rules |
| 04.02 | `backend/src/main/java/com/example/catalog/product/Category.java` | The six categories |
| 04.03 | `backend/src/main/java/com/example/catalog/product/Product.java` | JPA entity |
| 04.04 | `backend/src/main/java/com/example/catalog/product/ProductRepository.java` | Spring Data repository |
| 04.05 | `backend/src/main/resources/db/demo/demo-products.sql` | 24 demo products |
| 04.06 | `backend/src/main/java/com/example/catalog/demo/DemoDataLoader.java` | Dev-only loader |
| 04.07 | `backend/src/test/java/com/example/catalog/TestcontainersConfiguration.java` | Shared MySQL test container |
| 04.08 | `backend/src/test/java/com/example/catalog/product/ProductRepositoryTest.java` | Persistence tests |
| 04.09 | `backend/src/test/java/com/example/catalog/demo/DemoDataLoaderTest.java` | Demo-data tests |

Modified: `pom.xml` (02.01: persistence and Testcontainers dependencies, sections 02.01.08–09), `application.yml`
(02.06: JPA and Flyway settings, sections 02.06.03–04), `application-dev.yml` (02.07: the MySQL connection, section
02.07.04), and `HealthEndpointTest.java` (02.09: each configuration imports the test container).

**Relevant syntax**
- *SQL DDL*: `CREATE TABLE`, column types, `NOT NULL`, expression defaults `DEFAULT (UTC_TIMESTAMP(6))`, `PRIMARY KEY`,
  named `CONSTRAINT … UNIQUE` / `CHECK`, `REGEXP_LIKE(…, 'c')` for a case-sensitive pattern, and `ENGINE` / `CHARSET` / `COLLATE`.
- *Flyway file names*: `V1__create_product_table.sql`, meaning version 1, two underscores, then a description.
- *JPA*: `@Entity`, `@Table`, `@Id`, `@GeneratedValue(IDENTITY)`, `@Column`, `@Enumerated(STRING)`, `@Version`,
  `@PrePersist`, `@PreUpdate`.
- *Spring Data*: `interface ProductRepository extends JpaRepository<Product, Long>` and the derived query
  `Optional<Product> findBySku(String sku)`.
- *Spring*: `@Component`, `@Profile("dev")`, `ApplicationRunner`, constructor injection.
- *Configuration*: `spring.config.import: optional:file:../infra/.env[.properties]`, `${NAME:default}`, and environment
  variables such as `SPRING_DATASOURCE_URL` mapping onto `spring.datasource.url`.
- *Tests*: `@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)`, `@TestConfiguration`, `@Import`,
  `@ServiceConnection`, `@ParameterizedTest` with a `@CsvSource` text block, and AssertJ's `assertThatThrownBy(…).rootCause()`.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| start.spring.io metadata check | Confirm Spring Boot 4.1.1 is still the current GA release |
| `.\scripts\dev-services.ps1 up` | Start MySQL and Redis |
| `.\mvnw.cmd dependency:build-classpath` (test scope), then a search of the jars | Find where Boot 4 moved the test annotations (for example `@DataJpaTest`) |
| `.\mvnw.cmd -B -ntp clean verify` (twice) | Build and run every test against a real MySQL test container |
| `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"` (twice) | Migrate the development database and load the demo data, then confirm nothing runs twice |
| SQL piped into `docker compose … exec -T mysql` | Inspect `flyway_schema_history`, constraints, indexes, demo rows, and stored timestamps |
| `java -jar … --spring.profiles.active=prod` with `SPRING_DATASOURCE_*` variables | Production-style run of the packaged jar |

**Expected behavior.** Flyway creates the table once; Hibernate's validation passes; invalid rows are rejected by MySQL
itself; timestamps are stored in UTC; demo data appears only with `dev`, only once; the jar runs with `prod` when the
database settings come from environment variables.

**Actual checks** (2026-10-20)

| Check | Result |
|---|---|
| Versions | Spring Boot 4.1.1 still the GA release. Resolved: Hibernate 7.4.5, Flyway 12.4.0, Connector/J 9.7.0, Testcontainers 2.0.5, HikariCP 7.0.2 |
| First `clean verify` | 15 of 21 passed. The 6 failures were the CHECK-constraint cases: MySQL rejected every row with the right constraint (error 3819), but `JdbcTemplate` reported `UncategorizedSQLException`, not `DataIntegrityViolationException`. The tests now assert the actual behavior, and a new test shows that the JPA path does produce `DataIntegrityViolationException` |
| Second `clean verify` | 21 tests, 0 failures; one MySQL container served all six Spring test contexts |
| First dev run | Flyway applied V1 in 43 ms; Hibernate validated the schema; started in 5.4 s; 24 demo products loaded; health `db` UP (MySQL) |
| Second dev run | "Schema `catalog` is up to date"; "Demo data not loaded: the product table already has 24 rows"; still 24 rows |
| Database inspection | `flyway_schema_history`: V1 succeeded, checksum recorded. Constraints: 6 CHECK, PRIMARY, `uk_product_sku`. Indexes: only PRIMARY and `uk_product_sku`. Four products per category, two out of stock. A stored `created_at` was exactly 5 h 30 min behind the IST wall clock, so it is UTC |
| Packaged jar | Contains the three YAML files, the V1 migration, the demo SQL, the MySQL driver, and `flyway-mysql`; no test libraries |
| Production-style run | `prod` profile with the database from environment variables only; Flyway up to date; health without details; no demo data; no banner |

Findings:
- **How constraint errors surface.** Spring's built-in table of MySQL error codes lacks 3819, so plain `JdbcTemplate`
  reports a CHECK violation as a generic `UncategorizedSQLException`. Hibernate's MySQL dialect recognises it, so
  repository calls get `DataIntegrityViolationException`. Phase 05 maps the latter, because the API writes through JPA.
- **Maven 401 warnings.** Flyway's parent build file (`flyway-parent-12.4.0.pom`) declares two extra repositories, a
  GitHub Packages feed and a Redgate feed, that require login. Maven tried them, got HTTP 401, and resolved everything
  from Maven Central; the `_remote.repositories` records show `central` for each jar. Harmless, but a reminder that
  dependencies can add download sources.
- **Boot 4 moved test annotations**, for example `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`.
  Older tutorials show the Boot 3 packages.

**How the pieces connect**

```text
startup ─► Flyway runs db/migration/V1 (once) ─► Hibernate validates Product against the table ─► Tomcat starts
        └► with the dev profile: DemoDataLoader fills an empty table from db/demo/demo-products.sql
ProductRepository (interface) ─► implementation generated by Spring Data ─► Hibernate ─► HikariCP pool ─► MySQL
tests ─► TestcontainersConfiguration (one mysql:8.4.11) ─► @ServiceConnection ─► the same startup path
```

<!-- BUILD FLOW 01.05.07 — Phase 05 -->
## Phase 05 — CRUD API with DTOs, transactions, and one error format

**What we had before.** A product table, an entity, and a repository, but nothing a client could call.

**What we add.** `GET`, `POST`, `PUT`, and `DELETE` on `/api/v1/products/{id}`: request and response DTOs with Bean
Validation, an explicit mapper, a service that owns the transaction boundary and the business rules (write switch,
version check, error translation), a thin controller, one RFC 9457 error format for every failure, and two kinds of
tests: a fast web slice with a Mockito test double, and real-transaction tests on MySQL.

**Why.** The API is the contract the storefront (Phases 12–13), the cache (Phases 09–10), and the benchmarks
(Phase 11) build on. Its status codes, error shape, and conflict handling need to be settled and tested first.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 05.01 | `backend/src/main/java/com/example/catalog/config/CatalogProperties.java` | `catalog.writes-enabled` as a typed setting |
| 05.02 | `backend/src/main/java/com/example/catalog/product/ProductCreateRequest.java` | POST body with validation rules |
| 05.03 | `backend/src/main/java/com/example/catalog/product/ProductUpdateRequest.java` | PUT body plus `version` |
| 05.04 | `backend/src/main/java/com/example/catalog/product/ProductResponse.java` | What clients receive |
| 05.05 | `backend/src/main/java/com/example/catalog/product/ProductMapper.java` | Explicit mapping |
| 05.06 | `backend/src/main/java/com/example/catalog/product/ProductNotFoundException.java` | 404 |
| 05.07 | `backend/src/main/java/com/example/catalog/product/DuplicateSkuException.java` | 409 |
| 05.08 | `backend/src/main/java/com/example/catalog/product/VersionConflictException.java` | 409 |
| 05.09 | `backend/src/main/java/com/example/catalog/error/WritesDisabledException.java` | 403 |
| 05.10 | `backend/src/main/java/com/example/catalog/product/ProductService.java` | Rules and transactions |
| 05.11 | `backend/src/main/java/com/example/catalog/product/ProductController.java` | HTTP mapping |
| 05.12 | `backend/src/main/java/com/example/catalog/error/ApiExceptionHandler.java` | Problem Details for every error |
| 05.13 | `backend/src/test/java/com/example/catalog/product/ProductControllerTest.java` | Web-slice tests |
| 05.14 | `backend/src/test/java/com/example/catalog/product/ProductServiceTest.java` | Real-transaction tests |

Modified: `CatalogApplication.java` (02.05: `@ConfigurationPropertiesScan`, section 02.05.03); `application.yml`
(02.06.05), `application-dev.yml` (02.07.05), and `application-prod.yml` (02.08.03): the write switch; `pom.xml`
(02.01.10): Mockito as a Java agent.

**Relevant syntax**
- *Records*: `public record ProductResponse(long id, …) {}` is an immutable class with a generated constructor and
  accessors (`id()`, not `getId()`).
- *Bean Validation*: `@NotBlank`, `@Size(max = …)`, `@Pattern(regexp = …)`, `@NotNull`, `@DecimalMin`, `@Digits`,
  `@PositiveOrZero`, triggered by `@Valid` on a controller parameter.
- *Spring MVC*: `@RestController`, `@RequestMapping`, `@GetMapping("/{id}")`, `@PathVariable`, `@RequestBody`,
  `ResponseEntity.created(uri).body(…)`, `ResponseEntity.noContent()`.
- *Transactions*: `@Transactional`, `@Transactional(readOnly = true)`, and the rule "unchecked exception → rollback".
- *Errors*: `@RestControllerAdvice`, `@ExceptionHandler`, `ProblemDetail.forStatusAndDetail(…)`, and overriding
  `handleMethodArgumentNotValid` and `handleHttpMessageNotReadable` from `ResponseEntityExceptionHandler`.
- *Java*: pattern matching with `instanceof JacksonException jackson`, streams with `map`, `sorted`, and `toList()`.
- *Tests*: `@WebMvcTest`, `MockMvc` with `perform(post(…))`, `andExpect(status()…)`, `jsonPath(…)`;
  `@MockitoBean`, BDD-style `given(…).willReturn(…)` and `then(service).should()`; `@SpringBootTest(properties = …)`.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `.\mvnw.cmd dependency:build-classpath` and a jar search; `javap` | Find Boot 4's packages for `@WebMvcTest` and `@MockitoBean`, and Jackson 3's exception API |
| `.\mvnw.cmd -B -ntp clean verify` | Build and run all 43 tests |
| `.\scripts\dev-services.ps1 up` and `spring-boot:run` with `dev` | Run the API locally |
| `curl.exe` with JSON bodies in files | Exercise every status code by hand (files avoid PowerShell 5.1's quoting problem) |
| `java -jar … --spring.profiles.active=prod` with `SPRING_DATASOURCE_*` | Check that production refuses writes over HTTP |
| The README's `Invoke-RestMethod` commands, run verbatim | Make sure the documented examples work |

**Expected behavior.** 200, 201 with `Location`, and 204 for successful calls; 400 with field errors for invalid
input; 404, 409, and 403 with their own Problem Details types; failed writes leave the database unchanged.

**Actual checks** (2026-10-21)

| Check | Result |
|---|---|
| `clean verify` | 43 tests, 0 failures, on the first run: 14 controller, 8 service, 13 repository, 6 health, 2 demo data |
| Mockito agent | Both dynamic-agent warnings gone; one harmless Class Data Sharing notice remains (decision D-030) |
| Manual CRUD (dev) | GET 200; POST 201 with `Location: /api/v1/products/27`; same SKU 409; invalid body 400 listing three fields; unknown category 400 naming the six values; PUT with version 0 → 200 and version 1; the same PUT again → 409 "is at version 1, but this change was based on version 0"; DELETE 204; then GET 404; `/abc` 400; an unknown path 404 |
| Production mode over HTTP | GET 200; POST, PUT, and DELETE each 403 `writes-disabled`; the product unchanged afterwards |
| README examples | Create, update, and delete with `Invoke-RestMethod` worked verbatim (id 29, version 0 → 1, then 404) |

Mistakes and findings on the way:
- My first version of `ProductService` translated optimistic-lock failures inside the shared `saveAndFlush` helper
  using `product.getId()`, which is `null` on the create path; unboxing it would have thrown a
  `NullPointerException`. Caught in review before any run: the translation now lives in `updateProduct` and
  `deleteProduct`, the only places where it can happen.
- My first manual-test script mis-parsed response headers and printed blanks; the requests still ran. After the fix,
  the whole sequence was rerun cleanly.
- `AUTO_INCREMENT` gaps: a failed duplicate INSERT still consumes an id, so the next product got 27, not 26. Ids are
  identifiers, never counts.
- Windows PowerShell 5.1's `Invoke-RestMethod` hides Problem Details bodies on errors; the README points to `curl.exe`.
- Spring omits `type` when it is `about:blank`, which RFC 9457 allows.

**How the pieces connect**

```text
client ─► Tomcat ─► DispatcherServlet ─► Jackson reads JSON ─► @Valid (Bean Validation)
   ─► ProductController ─► ProductService (transaction proxy: begin ─ work ─ commit or rollback)
          ├─ CatalogProperties (write switch)   ├─ ProductRepository ─► Hibernate ─► MySQL
          └─ ProductMapper (DTO ⇄ entity)
   any exception ─► ApiExceptionHandler ─► application/problem+json
```

<!-- BUILD FLOW 01.05.08 — Phase 06 -->
## Phase 06 — Search with filters, stable sorting, and pagination

**What we had before.** Single products could be read and written, but there was no way to list or find products.

**What we add.** `GET /api/v1/products` with five optional filters (`q`, `category`, `minPrice`, `maxPrice`,
`inStock`), a sort allowlist whose orders all end with `id`, zero-based pages with validated bounds, and our own page
envelope. Filters become JPA predicates with bound parameters (Spring Data JPA Specifications), and invalid
parameters get the same Problem Details as invalid bodies.

**Why.** The storefront (Phase 12) is built around this endpoint, and Phase 08 needs a real, unoptimized search query
to measure before adding indexes. Paging must be correct before it is fast: no product may appear twice or never.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 06.01 | `backend/src/main/java/com/example/catalog/product/ProductSort.java` | Sort allowlist; each order ends with `id` |
| 06.02 | `backend/src/main/java/com/example/catalog/product/ProductSortConverter.java` | `?sort=` text → `ProductSort` |
| 06.03 | `backend/src/main/java/com/example/catalog/product/ValidPriceRange.java` | Cross-field rule: `maxPrice` ≥ `minPrice` |
| 06.04 | `backend/src/main/java/com/example/catalog/product/ProductSearchRequest.java` | The query parameters, validated, with defaults |
| 06.05 | `backend/src/main/java/com/example/catalog/api/PageResponse.java` | The page envelope |
| 06.06 | `backend/src/main/java/com/example/catalog/product/ProductSpecifications.java` | Filters → predicates; LIKE escaping |
| 06.07 | `backend/src/test/java/com/example/catalog/product/ProductSearchTest.java` | Search tests on MySQL |

Modified: `ProductRepository.java` (04.04.03: `JpaSpecificationExecutor`); `ProductService.java` (05.10.07:
`searchProducts`); `ProductController.java` (05.11.06: `GET /api/v1/products`); `ApiExceptionHandler.java` (05.12.04
and 05.12.08: neutral messages for query values that cannot be converted); `ProductControllerTest.java` (05.13.05:
eight search tests).

**Relevant syntax**
- *Spring Data JPA*: `Specification<Product>` as a lambda `(root, query, builder) -> builder.like(root.get("name"), pattern, '!')`,
  `Specification.allOf(list)`, `JpaSpecificationExecutor.findAll(spec, pageable)`, `PageRequest.of(page, size, sort)`,
  `Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))`, `page.map(mapper::toResponse)`.
- *Spring MVC*: `@ModelAttribute` binding query parameters to a record; a `Converter<String, ProductSort>` bean.
- *Bean Validation*: `@Min`, `@Max`, `@Size` on record components; a custom class-level constraint
  (`@Constraint(validatedBy = …)`, `ConstraintValidator`, `addPropertyNode("maxPrice")`).
- *Java*: a record's compact constructor (`public ProductSearchRequest { page = (page == null) ? 0 : page; … }`), an
  enum with fields and an overridden `toString()`, a generic record `PageResponse<T>`, `List.copyOf`.
- *Errors*: `FieldError.contains(TypeMismatchException.class)` and `unwrap(…)` to recognize values that failed conversion.
- *Tests*: `@Transactional` on a test class (rollback after each test), `JdbcTemplate.update`, `LAST_INSERT_ID()`,
  AssertJ `containsExactly`; MockMvc `.param(…)` and Hamcrest `contains`/`everyItem` on `jsonPath`.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `javap` on the Spring Data JPA 4.1 and Spring classes | Confirm the `Specification` and binding-error APIs before writing code |
| A search of the Spring Data jars for two messages | Confirm the offset limit (`PageableUtils`) and the warning about serializing `PageImpl` |
| `.\scripts\dev-services.ps1 up` | Start MySQL and Redis |
| `.\mvnw.cmd -B -ntp clean verify` | Build and run all 64 tests |
| `java -jar target\catalog-0.0.1-SNAPSHOT.jar` with `dev`, `--logging.level.org.hibernate.SQL=debug`, `--logging.level.org.hibernate.orm.jdbc.bind=trace`, and `--logging.file.name=…` | Run the API with every SQL statement and bound value logged |
| 19 searches with `curl.exe`, each followed by counting the new SQL lines in the log | Check results, errors, and the number of queries per request |
| The README's search example, run verbatim | Make sure the documented example works |

**Expected behavior.** Every combination of filters works; each sort orders by its main column and then by `id`;
pages never overlap; invalid parameters get a 400 naming them, and no SQL runs for them; search text is only ever a
bound value.

**Actual checks** (2026-10-21)

| Check | Result |
|---|---|
| `clean verify` | 64 tests, 0 failures, on the first run: 22 controller, 13 search, 13 repository, 8 service, 6 health, 2 demo data |
| Defaults | 24 products, 2 pages, 20 on the first; ids 24 down to 5, because all demo rows share one `created_at` and the `id` tie-breaker decides |
| Filters | `category=BAGS&sort=price_asc`: the four bags, cheapest first. `q=LINEN`: three names containing "Linen". `minPrice=1000&maxPrice=2000&inStock=true`: six products, the ₹1,290 tie in id order (1, then 19). `inStock=false`: the two sold-out products |
| SQL | `where p1_0.name like ? escape '!' order by p1_0.name,p1_0.id limit ?,?` with `[%LINEN%]` bound. No filters: no WHERE clause. `?q=x' OR '1'='1` was bound as the value `%x' OR '1'='1%` and found nothing. `?q=%` became `%!%%` and found nothing |
| Queries per request | Full first page: 2 (page + `COUNT`). Partial last page (page 2 of 10): 1, since 20 + 4 gives the total. Page past the end: 2. Invalid parameters: 0 |
| Errors | `size=500`, `page=-1`, `page=30000000`, `sort=popular`, `category=SHOES`, `minPrice=5000&maxPrice=1000`, and `minPrice=abc&page=x` each gave 400 `validation-failed` with the right fields and messages |
| README example | The `Invoke-RestMethod` search printed "2 products, page 1 of 1" with the sling bag and the tote |

Mistakes and findings on the way:
- A repeated parameter (`?category=BAGS&category=HOME`) silently uses the first value. Recorded in D-034 and the README.
- `?sort=popular&size=500` reported only `sort`: when a value cannot be converted, Spring skips the validation rules.
  My first code comments claimed every problem is always reported at once; they were corrected to match.
- The test MySQL container took 76 seconds to start on this run, with the development containers running at the same
  time; the tests themselves took a few seconds per class.

**How the pieces connect**

```text
GET /api/v1/products?q=linen&sort=price_asc
   ─► data binding: ProductSortConverter, defaults (compact constructor), @Valid + @ValidPriceRange
   ─► ProductController.searchProducts ─► ProductService.searchProducts (read-only transaction)
          ├─ ProductSpecifications.matching ─► WHERE … (bound parameters)
          ├─ ProductSort.toSort ─► ORDER BY …, id
          └─ ProductRepository.findAll(spec, PageRequest) ─► page query (+ COUNT when needed) ─► MySQL
   ─► ProductMapper + PageResponse.from ─► {content, page, size, totalElements, totalPages}
   invalid parameters ─► ApiExceptionHandler ─► 400 validation-failed (no SQL)
```

<!-- BUILD FLOW 01.05.09 — Phase 07 -->
## Phase 07 — Full-stack tests over real HTTP, and example requests

**What we had before.** Every layer was tested on its own: the web layer with a test double (MockMvc), the service
and search on MySQL without HTTP, the repository with a JPA slice. Only the health endpoint was called over a real
network connection.

**What we add.** Two test classes that start the whole application on a random port and call it over real HTTP, as
a client would. They cover a product's lifecycle, the exact JSON shape, every error type, a rollback seen from
outside, Unicode, two races of ten simultaneous requests, search from real query strings, and the read-only `prod`
profile. Plus an `.http` file of example requests.

**Why.** The seams between layers are where the separate tests cannot look: headers, unknown paths, percent-decoding,
JSON produced from real database rows, and requests that really run at the same time on different server threads.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 07.01 | `backend/src/test/java/com/example/catalog/product/ProductApiIntegrationTest.java` | Writes, errors, rollback, Unicode, races, read-only `prod` |
| 07.02 | `backend/src/test/java/com/example/catalog/product/ProductSearchApiIntegrationTest.java` | Search over HTTP |
| 07.03 | `backend/api-examples.http` | Example requests for VS Code's REST Client |

Modified: documentation only (D-031's wording corrected; D-036 added; `REQUEST_FLOWS.md` 01.07.07 maps each flow to its
tests). No production code changed: the full-stack tests found no defects.

**Relevant syntax**
- *Spring Boot tests*: `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = …)`,
  `@AutoConfigureRestTestClient`, `@LocalServerPort`.
- *RestTestClient*: `client.post().uri(…).contentType(…).body(record).exchange()`, then `expectStatus().isCreated()`,
  `expectHeader().contentType(…)`, `expectBody().jsonPath("$.type").isEqualTo(…)`,
  `expectBody(ProductResponse.class).returnResult().getResponseBody()`, `uri("…?q={q}", value)` (percent-encodes the
  value), `client.method(HttpMethod.PATCH)`.
- *Generic bodies*: `new ParameterizedTypeReference<PageResponse<ProductResponse>>() {}`.
- *Concurrency*: `ExecutorService` in try-with-resources (Java 21), two `CountDownLatch`es to release ten requests
  together, `Future.get(30, SECONDS)`.
- *Test data*: committed rows, so unique SKUs (`API-…`, `SRCHAPI-…`) and per-test name prefixes (`Zq4 Filter`, …).
- *`.http` files*: `@baseUrl = …`, `###` between requests, `# @name createProduct`,
  `{{createProduct.response.body.$.id}}`.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `javap` on `RestTestClient`, its assertion classes, and Boot's `RestTestClientTestAutoConfiguration` | Confirm the API and how the client finds the random port (and the MockMvc trap) |
| `.\mvnw.cmd dependency:tree -Dincludes=…resttestclient` | Confirm which starter provides the Boot integration |
| `.\mvnw.cmd -B -ntp clean verify` | Build and run all 85 tests |
| `.\scripts\dev-services.ps1 up` and `java -jar … --spring.profiles.active=dev` | A real dev server for the manual checks |
| A small runner that sends each request of `api-examples.http` with `curl.exe` | Check every example request against the dev server |
| `curl.exe -Z --parallel-immediate --parallel-max 10` with ten PUTs of version 0, three rounds | Watch a race by hand, and see which version check stops the losers |

**Expected behavior.** Every test passes against the real server; the race tests end with exactly one winner however
the requests interleave; the example requests produce the statuses their titles promise.

**Actual checks** (2026-10-22)

| Check | Result |
|---|---|
| `clean verify` | 85 tests, 0 failures, on the first run: 22 controller, 15 API, 13 search, 13 repository, 8 service, 6 search API, 6 health, 2 demo data. `ProductSearchApiIntegrationTest` reused the application of `ProductApiIntegrationTest.WithWritesEnabled` (1.4 seconds for the class) |
| Applications and pools | 5 real servers and 10 connection pools for the whole run (8 before this phase) |
| Race tests | Ten simultaneous updates of version 0: one 200 and nine 409 `version-conflict`. Ten simultaneous creates with one SKU: one 201, nine 409 `duplicate-sku`, and one row in the table |
| `api-examples.http` | All 10 requests answered as their titles say: three searches (200), invalid parameters (400 naming `maxPrice` and `size`), create (201, id 30), read (200), update (200, version 1), the same update (409), delete (204), read (404) |
| Manual race, three rounds | Each round: one 200 (version 1) and nine 409s, every one of them from the second check (`UPDATE … WHERE version = ?` matched no row) |

Mistakes and findings on the way:
- The manual race showed something the automated test cannot: all ten requests read version 0 before the winner
  committed, so the service's own version comparison let all ten through. Only the database stopped the nine losers:
  their UPDATEs waited for the winner's row lock and then found no row with version 0. Without `@Version`, all ten
  would have answered 200, each overwriting the one before.
- My first `.http` draft put a section comment after a GET request. In an `.http` file every line after the first
  blank line of a request is body, so that comment would have been sent as the GET's body. It now sits before a
  request line, and the runner checks that no request has a body that is not JSON.
- D-031 claimed that a single `(:q IS NULL OR …)` query makes index use harder for MySQL. That overstated it: MySQL can
  drop such conditions when the driver inlines the values. The wording now describes the real risks.
- Each test configuration keeps its own connection pool against the one test MySQL: 10 pools now, against MySQL's
  default limit of 151 connections. Recorded as a limit to watch (D-036).

**How the pieces connect**

```text
Test thread(s) ─► RestTestClient ─► real TCP connection to localhost:<random port>
   ─► Tomcat ─► DispatcherServlet ─► ProductController ─► ProductService (real transaction) ─► MySQL container
   ◄─ status, headers, JSON (or Problem Details) ◄─ checked with expectStatus / expectHeader / jsonPath
Races: 10 threads ─► latch released ─► 10 requests at once ─► one commits; the database stops the other nine
```

<!-- BUILD FLOW 01.05.10 — Phase 08 -->
## Phase 08 — Measured query optimization

**What we had before.** Correct search with no indexes beyond the primary key and the unique SKU, tested on a few
dozen rows.

**What we add.**
- A separate benchmark database (`catalog_bench`) with 100,000 generated, reproducible products, loaded by a
  `bench`-only seeder.
- Tooling to capture the SQL the application really sends and to measure it with `EXPLAIN ANALYZE`.
- Measurements before and after migration `V2`, which adds four indexes chosen from those plans.
- "What if" checks per index, and `docs/QUERY_OPTIMIZATION.md`.

**Why.** Indexes are a trade: faster reads, slower writes, more storage. Choosing them from measured plans, and
proving that results did not change, is the difference between optimizing and guessing.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 08.01 | `backend/src/main/resources/application-bench.yml` | The `bench` profile |
| 08.02 | `scripts/bench-db.ps1` | Benchmark database and query log |
| 08.03 | `backend/src/main/java/com/example/catalog/bench/BenchmarkProperties.java` | `catalog.bench.*` settings |
| 08.04 | `backend/src/main/java/com/example/catalog/bench/BenchmarkProductGenerator.java` | Reproducible products |
| 08.05 | `backend/src/main/java/com/example/catalog/bench/BenchmarkDataSeeder.java` | Loads them (bench only) |
| 08.06 | `backend/src/test/java/com/example/catalog/bench/BenchmarkProductGeneratorTest.java` | Determinism, validity, shape |
| 08.07 | `backend/src/test/java/com/example/catalog/bench/BenchmarkDataSeederTest.java` | Loading rules |
| 08.08 | `benchmarks/queries/search-queries.sql` | The captured SQL |
| 08.09 | `scripts/measure-queries.ps1` | `EXPLAIN ANALYZE` and result fingerprints |
| 08.10 | `benchmarks/query-plans/before-v2.txt` | Measurements before (generated) |
| 08.11 | `backend/src/main/resources/db/migration/V2__add_search_indexes.sql` | The four indexes |
| 08.12 | `benchmarks/query-plans/after-v2.txt` | Measurements after, compared (generated) |
| 08.13 | `benchmarks/queries/index-alternatives.sql` | "What if" variants with index hints |
| 08.14 | `benchmarks/query-plans/index-alternatives.txt` | Their measurements (generated) |
| 08.15 | `docs/QUERY_OPTIMIZATION.md` | Method, evidence, decisions |

Modified: documentation only (README, this guide, FILE_GUIDE, REQUEST_FLOWS `SEARCH-05`, DECISIONS D-037 to D-041,
LEARNING_LOG, PROGRESS).

**Relevant syntax**
- *Spring*: a profile-specific YAML file; `@ConfigurationProperties` with `@DefaultValue` and `@Validated`;
  `TransactionTemplate.executeWithoutResult(…)`; `JdbcTemplate.batchUpdate(sql, items, batchSize, setter)`;
  `--spring.flyway.target=1` to stop Flyway at a version.
- *Java*: `java.util.Random(seed)`, `String.format(Locale.ROOT, …)`, `Stream.generate(…).limit(n)`,
  `MessageDigest` (SHA-256), `HexFormat`, Bean Validation's `Validation.buildDefaultValidatorFactory()` in a unit test.
- *MySQL*: `EXPLAIN ANALYZE`, `ANALYZE TABLE`, `ALTER TABLE … ADD INDEX …, ALGORITHM = INPLACE, LOCK = NONE`,
  the general query log (`SET GLOBAL general_log`), `IGNORE INDEX`, `FORCE INDEX`, and `/*+ SET_VAR(…) */` hints,
  `information_schema.statistics`, `mysql.innodb_index_stats`.
- *PowerShell*: running SQL inside the container through standard input; `[ordered]@{}`; SHA-256 with .NET.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `netsh interface ipv4 show excludedportrange protocol=tcp`, `Get-NetTCPConnection`, a search of other projects' configs | Check that port 24083 is free for the `bench` profile |
| `.\mvnw.cmd test "-Dtest=BenchmarkProductGeneratorTest"` | Record the dataset fingerprint (see the findings) |
| `.\mvnw.cmd -B -ntp clean verify` | Build and run all tests, before and after V2 existed |
| `.\scripts\bench-db.ps1 create`, `status`, `reset -Force`, `drop -Force` | Manage the benchmark database |
| `java -jar … --spring.profiles.active=bench` (and `--spring.flyway.target=1`) | Migrate and load the benchmark data |
| `.\scripts\bench-db.ps1 log-on`, 8 `curl.exe` searches, `log-show`, `log-off` | Capture the application's SQL |
| `.\scripts\measure-queries.ps1 -Label before-v2`, `-Label after-v2 -Baseline before-v2`, `-Label index-alternatives -QueryFile …` | Measure |
| One scripted run: fresh database, V1 only, load and measure; load again; V2; two loads with the indexes; measure | Reproduce everything from scratch and time loads with and without the indexes |

**Expected behavior.** Large speed-ups only where an index can deliver rows in order or narrow them; no change where
it cannot; identical results everywhere.

**Actual checks** (2026-10-22)

| Check | Result |
|---|---|
| Dataset | 100,000 products valid by the API's rules; shares as designed; fingerprint `6b78f508…462c367f`, pinned by a test |
| `clean verify` before V2 existed | 91 tests, 0 failures (85 earlier, 4 generator, 2 seeder) |
| First benchmark load | Flyway V1 in 95 ms; 100,000 products in 3,780 ms |
| SQL capture | 8 requests produced 15 statements, 12 distinct; the no-match search ran no count, as Phase 06 predicted |
| Before V2 | Every page query read all 100,000 rows and sorted: 37–49 ms; the deep page 89–91 ms |
| V2 | Applied online over 100,000 rows in 3.4 s |
| After V2 | Page queries 0.08–0.66 ms (73–483× faster); category count 5.8×; other counts and the deep page unchanged; the no-match search 44.5 → 193 ms; every result identical |
| "What if" | Without `(category, created_at)`: 0.273 ms (reads 97 entries); the no-match search without `(created_at)`: 44.4 ms; with `prefer_ordering_index=off`: 186 ms; the deep page forced onto `(created_at)`: 170 ms |
| Full reproduction run | Fresh database with `--spring.flyway.target=1`, through V2: identical fingerprints throughout. Loads took 3.8–5.6 s without and 4.6–6.4 s with the indexes; index space 12.1 MB when built in bulk, 18.1 MB when grown row by row |
| `clean verify` with V2 | 91 tests, 0 failures; the test container applied V1 and V2 ("now at version v2"); 11 connection pools for the run |

Mistakes and findings on the way:
- **Recording the fingerprint.** It can only be learned from a first generation, so the test first ran with a
  placeholder. As intended, that one test failed and printed the real value, which the test now pins. The other three
  generator tests passed in that same run.
- **A machine path leaked into a measurement file.** PowerShell variable names ignore case: `$queryFile` overwrote the
  parameter `$QueryFile`, and the file header showed the full local path. It was noticed in the output and fixed with
  a distinct name before any file was committed; the pre-commit check also scans for such paths.
- **Stale sizes.** MySQL caches table statistics, so the first "after" header still showed the index size from before
  V2. The script now runs `ANALYZE TABLE` before measuring.
- **A helper script error caught before running.** My first draft of the full-run helper would have captured log
  output into a function's return value (a PowerShell trap). It was fixed before the run, and given a `finally` that
  stops the Java process it started even if a step fails.
- **Every number re-measured from scratch.** `before-v2.txt` was regenerated during the full reproduction run, after
  the script fixes above. The first measurement agreed within a few milliseconds (home page 42.3 ms then, 42.2 ms
  now), with identical fingerprints.
- **The no-match regression is real and kept** (D-041). It was found by measuring every query, not only the ones
  expected to improve.
- **Write cost was not measurable** with a few 100,000-row loads: the timings with and without the indexes overlap.
  The build time (3.4 s) and the space (12–18 MB) are what was measured.

**How the pieces connect**

```text
bench-db.ps1 create ─► catalog_bench ─► java -jar … bench ─► Flyway V1 (V2) ─► BenchmarkDataSeeder (100,000 rows)
8 API requests ─► general query log ─► search-queries.sql (12 statements)
measure-queries.ps1 ─► ANALYZE TABLE ─► per query: warm-up, EXPLAIN ANALYZE ×3 (median), SHA-256 of the result
   before-v2.txt ─► V2__add_search_indexes.sql ─► after-v2.txt (compared: identical) ─► index-alternatives.txt
   ─► docs/QUERY_OPTIMIZATION.md
```

<!-- BUILD FLOW 01.05.11 — Phase 09 -->
## Phase 09 — Redis cache for product details (read path)

**What we had before.** Every `GET /api/v1/products/{id}` read MySQL. Redis ran in Docker Compose since Phase 03,
unused.

**What we add.** Cache-aside for product details: look in Redis, on a miss read MySQL and store the JSON for 60
seconds, count hits, misses, and errors. Versioned keys (`catalog:v1:product:{id}`), the application's own JSON, a
switch, Redis excluded from health, and tests against a real Redis container. The cache ships **switched off**:
writes do not clear it until Phase 10 (D-044).

**Why.** Product pages are read far more often than products change; a hit skips the database entirely. Building the
read path first, with tests, keeps Phase 10 focused on the hard part: clearing the cache at exactly the right moment.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 09.01 | `backend/src/main/java/com/example/catalog/config/ProductCacheProperties.java` | The switch and the TTL |
| 09.02 | `backend/src/main/java/com/example/catalog/product/ProductDetailCache.java` | Redis get/put, JSON, counters |
| 09.03 | `backend/src/test/java/com/example/catalog/product/ProductDetailCacheTest.java` | Cache tests on real Redis |
| 09.04 | `backend/src/test/java/com/example/catalog/TestcontainersConfigurationTest.java` | Checks the shared test setup |
| 09.05 | `docs/CACHING.md` | The cache explained, with its limits |

Modified: `pom.xml` (02.01.11–02.01.12: Spring Data Redis and its test starter); `application.yml` (02.06.06–02.06.07:
cache settings, Redis out of health); `application-dev.yml` (02.07.06–02.07.07: Redis address, `/actuator/metrics`);
`ProductService.java` (05.10.02: cache-aside, a `TransactionTemplate` around the MySQL read only);
`TestcontainersConfiguration.java` (04.07.03–04.07.04: a Redis container, small idle pools).

**Relevant syntax**
- *Spring Data Redis*: `StringRedisTemplate.opsForValue().get(key)` and `.set(key, value, ttl)`, `getExpire`, `hasKey`.
- *Jackson 3*: `JsonMapper.writeValueAsString(…)`, `readValue(json, ProductResponse.class)`, `JacksonException`.
- *Micrometer*: `Counter.builder("catalog.product.cache").tag("result", "hit").register(meters)`.
- *Transactions*: `new TransactionTemplate(transactionManager)`, `setReadOnly(true)`, `execute(status -> …)`.
- *Configuration*: `Duration` properties (`60s`), `@DefaultValue`, `management.health.redis.enabled`.
- *Tests*: `@ServiceConnection(name = "redis")` on a `GenericContainer`, `DynamicPropertyRegistrar`,
  `@AutoConfigureMetrics`, Awaitility's `await().atMost(…).until(…)`.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `.\mvnw.cmd dependency:build-classpath`, `javap` on the Redis connection factory and health configuration | Confirm Boot 4's Redis test support and the health property |
| `.\mvnw.cmd -B -ntp clean verify` | Build and run all tests (twice; see the findings) |
| `.\scripts\dev-services.ps1 up`, then `java -jar … --spring.profiles.active=dev --catalog.cache.enabled=true` | The dev stack with the cache on |
| `curl.exe` reads, `redis-cli --scan`, `TTL`, `GET` inside the Redis container, `/actuator/metrics/catalog.product.cache` | Inspect the cache by hand |

**Expected behavior.** A first read stores the product; a second read is a hit; nothing is stored for a missing
product; health stays UP without mentioning Redis; all earlier tests stay green with the cache off.

**Actual checks** (2026-10-23)

| Check | Result |
|---|---|
| First `clean verify` | 72 errors: no test application could start (see the findings) |
| Second `clean verify` | 100 tests, 0 failures (91 earlier, 7 cache, 2 setup); 13 connection pools, each keeping one idle connection |
| Manual check | One key `catalog:v1:product:7` after the first read, TTL 59 s, value exactly the API's JSON; metrics hit 1, miss 1, error 0; the unknown id 404 with no key; health UP with components db, diskSpace, livenessState, ping, readinessState, ssl |
| Dev database | Flyway applied V2 to `catalog` on this start (192 ms) |

Mistakes and findings on the way:
- **Docker Desktop was not running** at the start, and is installed per user rather than under Program Files; it was
  started from its own install location before any test ran.
- **Every test application failed to start** on the first run: "No ConnectionDetails found" for the Redis container.
  Spring Boot recognizes typed containers, but for a plain `GenericContainer` the connection must be named:
  `@ServiceConnection(name = "redis")`. One line fixed all 72 errors.
- **The first request's time is not a cache result.** 0.71 s versus 0.02 s for the second read mostly measures a cold
  JVM (class loading, compilation). Phase 11 compares cache on and off with warm-up runs.
- **Redis outages are not handled yet.** Lettuce waits up to 60 seconds for a command by default; Phase 10 adds short
  timeouts and tests the outage.

**How the pieces connect**

```text
GET /api/v1/products/7 ─► ProductController ─► ProductService.getProduct
   ├─ ProductDetailCache.get ─► Redis GET catalog:v1:product:7 ─► hit: return the JSON as ProductResponse
   └─ miss: TransactionTemplate (read-only) ─► ProductRepository.findById ─► MySQL ─► ProductMapper
            ─► ProductDetailCache.put ─► Redis SET … EX 60 ─► return
   counters: catalog.product.cache{result=hit|miss|error}
```

<!-- BUILD FLOW 01.05.12 — Phase 10 -->
## Phase 10 — Cache eviction after commit, and Redis failures

**What we had before.** A tested read-through-the-cache path, switched off because writes did not clear cached copies,
and a Redis client that would wait up to 60 seconds for an answer.

**What we add.** Every committed create, update, and delete evicts the product's key through a transactional event
(after the commit, never after a rollback); 200 ms Redis timeouts with MySQL as the fallback; eviction counters; the
cache switched on; tests for invalidation, rollback, the documented race, and a real Redis outage.

**Why.** A cache is only worth having if it never serves a change that was rolled back, rarely serves an outdated one,
and never makes the service fail when the cache itself fails.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 10.01 | `backend/src/main/java/com/example/catalog/product/ProductChangedEvent.java` | "Product N changed" |
| 10.02 | `backend/src/main/java/com/example/catalog/product/ProductCacheEvictionListener.java` | Evicts after the commit |
| 10.03 | `backend/src/test/java/com/example/catalog/product/ProductCacheInvalidationTest.java` | Eviction, rollback, the race |
| 10.04 | `backend/src/test/java/com/example/catalog/product/ProductCacheOutageTest.java` | Redis paused mid-test |

Modified: `ProductDetailCache.java` (09.02.05: `evict` and its counters); `ProductService.java` (05.10.08: writes
publish `ProductChangedEvent`); `application.yml` (02.06.06: cache on; 02.06.08: 200 ms timeouts);
`application-bench.yml` (08.01.04: the local Redis address); `ProductDetailCacheTest.java` (09.03.05: switches the
cache off explicitly); `docs/CACHING.md` (rewritten for writes, failures, and limits); README (status, tests, and the
production-style run with Redis variables).

**Relevant syntax**
- *Events*: `ApplicationEventPublisher.publishEvent(new ProductChangedEvent(id))`,
  `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- *Redis*: `StringRedisTemplate.delete(key)`; `spring.data.redis.timeout` and `connect-timeout` as durations.
- *Testcontainers*: `container.getDockerClient().pauseContainerCmd(id).exec()` and `unpauseContainerCmd`, always
  in `finally`; `@Qualifier("redisContainer")` to pick one of two container beans.
- *Spring internals (read with `javap`)*: `TransactionalApplicationListenerSynchronization` runs after-commit
  listeners in `afterCompletion`; `TransactionSynchronizationUtils.invokeAfterCompletion` logs their exceptions.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `javap -c` on Spring's transactional event classes | Confirm what happens to an exception in an after-commit listener |
| `.\mvnw.cmd -B -ntp clean verify` | Build and run all 107 tests |
| `java -jar … --spring.profiles.active=dev`, a scripted create, read, update, read with `curl.exe`; `docker compose stop redis`, reads and an update, `docker compose start redis`, delete; the metrics endpoint | Eviction and a stopped Redis on the real dev stack |
| The README's production-style block with the new `SPRING_DATA_REDIS_*` variables | Make sure the documented commands work |

**Expected behavior.** A write removes the key once it has committed and never after a rollback; with Redis down,
every request still succeeds within a fraction of a second; failed evictions are counted, never reported as failed
writes.

**Actual checks** (2026-10-29)

| Check | Result |
|---|---|
| `clean verify` | 107 tests, 0 failures, on the first run (100 earlier, now all with the cache on; 5 invalidation; 2 outage) |
| Outage test log | Each cache step gave up after about 200–230 ms with `QueryTimeoutException: Redis command timed out`; one failed eviction logged; no "afterCompletion threw exception" anywhere |
| Dev stack, Redis up | POST 201; first GET 90 ms (miss), second 14 ms (hit), TTL 59; after PUT the key was gone at once and the next GET showed version 1 |
| Dev stack, Redis stopped | Three GETs 430–435 ms each with correct data; PUT 200 in 253 ms (version 2; its eviction failed and was counted); GET showed the new name |
| Redis started again | GET 55 ms (miss), then 9 ms (hit); DELETE 204 removed the key; GET 404 |
| Metrics after the script | hit 2, miss 4, error 8; evictions done 3, failed 1 |
| README production-style run | Health UP without details; GET 200 twice with the key in Redis; POST 403; no cache warnings |

Mistakes and findings on the way:
- **A timeout is not a cancellation.** Commands sent to a paused Redis still run once it resumes, so a "failed"
  eviction can happen later after all. The outage test therefore checks behavior during the outage and uses fresh keys
  afterwards.
- **The failed eviction in the manual run left nothing stale**, but only because the dev Redis keeps no data across a
  restart. A Redis that merely hung would have kept the old copy until its TTL ended; `docs/CACHING.md` says so.
- **Two of my own helper lines failed**: a log filter cut strings by the original line's length, and a stop command
  looked up the process by its port and found nothing. Both were noticed in their output; the jar was then stopped by
  its command line, and every port was checked free.

**How the pieces connect**

```text
PUT /api/v1/products/42 ─► ProductService.updateProduct (transaction)
   ─► UPDATE … WHERE version = ? ─► publishEvent(ProductChangedEvent(42)) ─► commit
   ─► Spring afterCompletion(COMMITTED) ─► ProductCacheEvictionListener ─► ProductDetailCache.evict ─► Redis DEL
   rollback instead? the listener never runs
Redis hung or down: every Redis step gives up after 200 ms ─► counted ─► MySQL answers; writes still succeed
```

<!-- BUILD FLOW 01.05.13 — Phase 11 -->
## Phase 11 — Benchmarks and evidence with k6

**What we had before.** Search queries measured inside MySQL (Phase 08), and a cache tested for correctness
(Phases 09–10), but no measurement of whole HTTP requests under load.

**What we add.** Two k6 scripts; one script that runs the whole protocol and decides whether a run counts; the raw
results with a report computed from them; and `docs/PERFORMANCE.md`.

**Why.** "The cache makes it faster" is a claim until someone measures it. The measurement must be one that anyone
can reproduce, with every number traceable to a raw file. Measuring inside MySQL alone leaves out the network, the
JVM, JSON, and the cache itself.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 11.01 | `benchmarks/k6/product-detail.js` | One product by id: timed load, or one pass over the ids |
| 11.02 | `benchmarks/k6/search.js` | Four searches in rotation, each reported separately |
| 11.03 | `scripts/run-benchmarks.ps1` | The protocol, its validity checks, the result files |
| 11.04 | `benchmarks/results/k6-summaries.json` | **(generated)** Setup, attempts set aside, every trial's raw summary |
| 11.05 | `benchmarks/results/k6-report.md` | **(generated)** Medians and ranges computed from 11.04 |
| 11.06 | `docs/PERFORMANCE.md` | Method, results, meaning, limits, discarded runs |

Modified: README (status, what works, performance evidence, documentation map); `docs/CACHING.md` (status line);
`docs/DECISIONS.md` (D-048 method, D-049 when a run counts); `docs/FILE_GUIDE.md`; `docs/PROGRESS.md`;
`docs/LEARNING_LOG.md`. No backend code changed: the two benchmark-only settings are passed on the command line.

**Relevant syntax**
- *k6 options*:
  - `scenarios: { load: { executor: 'constant-vus', vus, duration } }` for timed load; `shared-iterations` for exactly
    1,000 requests.
  - `exec.vu.idInTest` and `exec.scenario.iterationInTest` pick the id.
  - `{ tags: { name } }` plus a threshold per tag makes k6 report each search separately.
  - `handleSummary(data)` writes the summary file; `discardResponseBodies` saves memory.
- *k6 metrics*:
  - `http_req_duration` is sending + waiting + receiving, **without** connecting.
  - `http_req_connecting` and `iteration_duration` show what that one leaves out.
  - In the summary, `http_req_failed.values.passes` is the number of failed requests.
  - `state.testRunDurationMs` is the real length of the run.
- *Docker*: `docker run --rm -v <k6Dir>:/scripts:ro -v <work>:/results -e NAME=value grafana/k6:2.3.0 run --quiet
  /scripts/<file>.js`; the script reads `__ENV.NAME`.
- *Windows*:
  - `SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED | ES_DISPLAY_REQUIRED)`, called through `Add-Type`,
    keeps the computer awake.
  - `Get-WinEvent -FilterHashtable` finds Kernel-Power events 42 (sleep) and 506 (Modern Standby).
  - `Win32_Battery.BatteryStatus` is 1 when the battery is discharging.
- *Spring Boot*: `--server.tomcat.max-keep-alive-requests=-1` keeps connections open for good.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `.\scripts\run-benchmarks.ps1 -Trials 1 -TrialDuration 3s -WarmupDuration 2s -SkipIndexComparison` | A short end-to-end run of the protocol and the report |
| `.\scripts\run-benchmarks.ps1`, five times | Full runs: four discarded, the fifth published (`docs/PERFORMANCE.md`) |
| `Get-WinEvent` for Kernel-Power events; `powercfg /a` | Find why the first run took three hours |
| k6 against a closed port | See the exact warning k6 writes when a connection can't be opened |
| Product-detail probes of 60 s and 120 s with k6, the second with `max-keep-alive-requests=-1`; `Get-NetTCPConnection -State TimeWait` | Reproduce the connection stalls, then show that open connections remove them |
| `redis-cli` by hand: `SET`, the script's `DEL`, `EXISTS`; `INFO stats` | Find why the cold passes weren't cold; confirm that Redis evicted nothing |

**Expected behavior.** With the cache warm, product detail should be clearly faster than with it off, and a miss
should be slower than a cache-off read. Search should be faster with the indexes, but by much less than inside MySQL,
because of the counts. Every number must come from the result files.

**Actual checks** (2026-10-29 and 2026-10-30)

| Check | Result |
|---|---|
| Published run (2026-10-30, 18:32) | 9.6 minutes; 15 trials, each on its first attempt; no failed request; trials lasted 30.0–30.1 s; slowest connection 21.7 ms |
| Cold passes | 0 hits and 1,000 misses each |
| Warm trials | 302,285 hits, 1,162 misses (all in trial 2, when the 60-second TTL ran out), no errors; Redis `evicted_keys:0` |
| Little's law (10 users ÷ mean iteration time) | Matches every measured rate within 1% |
| Script rules on synthetic k6 results | Eight cases: clean, overrun, two kinds of failed connection, slow connection, and a pass without a duration are judged correctly; a timeout after connecting and error statuses stop the run |
| Key deletion on the dev Redis | Keys in all four batches removed, an unrelated key kept, an error answer from Redis caught |
| After the runs | No backend process, no k6 container; ports 24081–24083 free |

Mistakes and findings on the way:
- **The laptop slept through the first run.** With Modern Standby, the display switching off puts the whole system
  into standby. Two 30-second trials lasted 54 minutes and two hours, and the report still looked plausible.
- **Connections hung inside Docker Desktop's forwarding.** Tomcat closes a connection after 100 requests. At hundreds
  of requests per second, k6 then opened several new connections a second, and a few took 19.5 s to open or timed out
  after 30 s. The measured request durations stayed normal. Only the run's length, `iteration_duration`, and
  `http_req_connecting` showed it.
- **A silent failure looked like a fast cache.** On a console set to UTF-8, Windows PowerShell 5.1 put a byte-order
  mark in front of the `DEL` command piped to `redis-cli`, so nothing was deleted. `redis-cli` still exited with 0.
  The "cold" passes were mostly hits, and their numbers looked like a great result. The cache counters exposed it.
- **Windows PowerShell 5.1's `ConvertTo-Json` fails on an empty list**, so the script builds the set-aside list's
  JSON by hand.
- **The published run drifted from trial to trial.** The laptop's other work and clock speed change over minutes.
  `docs/PERFORMANCE.md` shows the ranges and checks that the conclusions hold at their edges. The run was not
  repeated to get nicer numbers.

**How the pieces connect**

```text
run-benchmarks.ps1 (keeps the computer awake; refuses to start on battery)
  ├─ bench-db.ps1 drop/create ─► catalog_bench, Flyway V1 (target=1), later V2
  ├─ java -jar … bench profile + benchmark settings ─► 100,000 products seeded once
  ├─ docker run grafana/k6 ─► search.js / product-detail.js ─► host.docker.internal:24083 ─► backend
  │     └─ handleSummary ─► <label>.json, plus k6's output in <label>.out (scratch folder)
  ├─ /actuator/metrics before and after each product-detail trial; Clear-MeasuredKeys before each cold pass
  ├─ Get-Disturbance: count the trial, repeat it, or stop everything
  └─ all trials counted, no sleep logged ─► k6-summaries.json ─► k6-report.md ─► PERFORMANCE.md, README
```

<!-- BUILD FLOW 01.05.14 — Phase 12 -->
## Phase 12 — The storefront catalog

**What we had before.** A tested, measured API, but nothing for a shopper to look at.

**What we add.** The React storefront "FORM — The Everyday Edit":
- a header with search, a compact hero, and category tabs;
- filters: a sidebar on wide screens, a modal drawer on narrow ones;
- removable chips, sorting, pagination, and an accurate count;
- the whole search kept in the address bar;
- skeleton, empty, and error states;
- seven licensed photos, with a placeholder for the other products;
- tests and real-browser checks.

**Why.** The storefront is how anyone experiences the catalog. It has to be fast to use, honest about what is
loading or wrong, and shareable by URL.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 12.01–12.06 | `frontend/package.json`, `package-lock.json`, `vite.config.js`, `.oxlintrc.json`, `index.html`, `public/favicon.svg` | The project: dependencies, dev server and proxy, lint rules, the page |
| 12.07 | `frontend/src/main.jsx` | Starts React |
| 12.08–12.09 | `frontend/src/styles/tokens.css`, `app.css` | Design tokens, then every component's styles, mobile first |
| 12.10–12.11 | `frontend/src/lib/catalog.js`, `photoCredits.js` | Pure rules (URL, API query, prices, pages, INR) and the photo credits |
| 12.12 | `frontend/src/api/products.js` | The only network code; readable `ApiError`s |
| 12.13–12.15 | `frontend/src/hooks/useUrlState.js`, `useDebouncedCallback.js`, `useProductSearch.js` | State in the address bar, debouncing, fetching with cancellation |
| 12.16–12.28 | `frontend/src/components/*.jsx` | Header, hero, search, categories, filters, drawer, chips, toolbar, grid, card, pagination, status views, footer |
| 12.29 | `frontend/src/App.jsx` | The page that connects them |
| 12.30–12.32 | `frontend/src/test/setup.js`, `lib/catalog.test.js`, `App.test.jsx` | Tests |
| 12.33–12.39 | `frontend/public/images/products/*.jpg` | Seven photos |
| 12.40 | `docs/IMAGE_CREDITS.md` | Their sources and licenses |

Modified:
- **Backend.** `demo-products.sql` (04.05: seven `image_url` paths). `ProductCreateRequest` and
  `ProductUpdateRequest` (05.02, 05.03: `imageUrl` may be `/images/products/…`). `ProductControllerTest` (05.13: a
  test of that rule).
- **Docs.** `docs/DECISIONS.md` (D-050 to D-053; the open questions); `docs/PROJECT_PLAN.md` §4 (re-checked
  versions); `docs/REQUEST_FLOWS.md` (SEARCH-01 and SEARCH-07 made real); README (status, prerequisites, ports,
  quick start, tests, docs map); FILE_GUIDE; PROGRESS; LEARNING_LOG.

**Relevant syntax**
- *React*:
  - components are functions that return JSX; props come in as one object; `useState`, `useEffect` (with a cleanup
    function), `useRef`, `useMemo`, `useCallback`;
  - the "adjust state when a prop changes" pattern, as in `SearchBox`: compare with a stored value during rendering;
  - `key` on list items.
- *Browser APIs*:
  - the address bar: `URLSearchParams`, `history.pushState` / `replaceState`, the `popstate` event;
  - requests: `fetch` with an `AbortController` signal;
  - formatting: `Intl.NumberFormat('en-IN', {style: 'currency', currency: 'INR', trailingZeroDisplay: 'stripIfInteger'})`;
  - elements: `<dialog>` with `showModal()`, `<search>`, `<details>`.
- *CSS*:
  - custom properties (`var(--color-text)`), `@media (min-width: 48rem)`, `prefers-reduced-motion`;
  - `aspect-ratio`, `object-fit: contain`, `scroll-margin-top`;
  - attribute selectors such as `[aria-pressed="true"]`.
- *Testing*:
  - Vitest with jsdom;
  - Testing Library's `render`, `screen.findByRole` / `getByRole`, `userEvent.setup()`;
  - `vi.fn()` and `vi.stubGlobal('fetch', …)`.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `curl.exe -s https://nodejs.org/dist/index.json`; `npm view <package> version` | Re-check the version baseline |
| `npm create vite` with `create-vite` 9.3.3, the `react` template, in a scratch folder | Read the current template before writing the files by hand |
| The Wikimedia Commons API (`action=query`, `prop=imageinfo`, `extmetadata`), contact sheets drawn with .NET's `System.Drawing` | Find free-licensed photos and look at them |
| `npm install`, `npm run lint`, `npm test`, `npm run build` | Install, lint, test, build |
| `.\mvnw.cmd -B -ntp clean verify` | The backend with the widened `imageUrl` rule |
| `.\scripts\dev-services.ps1 reset -Force`, `up`; `java -jar … --spring.profiles.active=dev`; `npm run dev` | The real stack with the new demo data |
| A browser script (Playwright API, headless Chromium) at 375, 768, and 1440 pixels | Layout, behaviour, and screenshots in a real browser |

**Expected behavior.**
- Typing sends one request after the pause, and Back and Forward walk through searches.
- A filter change returns to page 1.
- A bad price range is explained, not sent.
- Errors read as sentences.
- Nothing scrolls sideways on a phone.

**Actual checks** (2026-10-30)

| Check | Result |
|---|---|
| Version re-check | Node 26 LTS since 2026-10-28; Vite 8.3.5 (a patch); the template lints with oxlint; the rest unchanged |
| `npm install` | 100 packages, 0 vulnerabilities reported |
| `npm run lint` / `npm test` / `npm run build` | Clean (after one fix, below) / 17 passed / 239 KB JavaScript (75 KB gzipped), 12.7 KB CSS |
| Backend `clean verify` | 108 tests, 0 failures |
| Real stack | Flyway built the schema from scratch; 24 demo products loaded; the page, `/api` through the proxy, and a photo answered 200 |
| Browser, 375 / 768 / 1440 px | No sideways scrolling; 2 / 3 / 3 columns; sidebar only at 1440, the Filters button below that; every photo on page 1 loaded (4 of 12; 8 placeholders) |
| Browser, behaviour | "linen": exactly one request, address `?q=linen`, 3 products. Back restored the search and the category. Sort high to low: ₹6,990, ₹6,490, … Page 2: "Showing 13–24 of 24 products", scrolled to the results below the header. Reversed prices: the message shown, no request sent. Invalid address: "Maximum price: must be greater than or equal to minPrice." `?q=zzzz` and `?page=9`: their own empty states |
| Browser, drawer and keyboard | The drawer opened as a modal with focus inside; "Show 22 products"; Escape closed it and focus returned to "Filters (1)"; the first Tab reached "Skip to products" |
| Console | The only error was the 400 the invalid-address check asked for. The aborted requests are React StrictMode's repeated first request in development, plus older requests cancelled on purpose |

Mistakes and findings on the way:
- **Photo sourcing.** Openverse timed out, and Commons mostly returned museum pieces, people, and other brands'
  products. Seven photos passed the rules; the rest show a designed placeholder (D-051).
- **The API rejected the data it served.** The demo rows needed `/images/products/…` paths, which `imageUrl` refused,
  so the rule was widened and tested (D-052).
- **Lint.** It asked for the `<search>` element instead of `role="search"`.
- **The browser check found a real gap.** While new results loaded, an old empty message stayed fully visible next to
  the new state. The busy state moved from the grid to the whole results area.
- **Two visual refinements:**
  - the hero line no longer repeats the header's tagline;
  - placeholders now share the photos' mat colour.
- **PowerShell 5.1 again:**
  - a script without a byte-order mark broke a non-ASCII file name;
  - `ConvertFrom-Json` returns an array as one object;
  - `Cli` is an alias of `Clear-Item`.

  They affected only scratch tools, and each was fixed there.

**How the pieces connect**

```text
address bar ?q=linen&page=2 ──► useUrlState (readSearchState) ──► search state
   typing ─► SearchBox (300 ms pause) ─┐   click ─► CategoryTabs / FilterPanel / ResultsToolbar / Pagination
                                       └──► update(changes) ─► pushState or replaceState ─► new state (page 1 unless paging)
search state ─► toApiQuery ─► useProductSearch: abort the old request, fetch /api/v1/products?… ─► Vite proxy ─► Spring Boot
answer ─► App: ErrorState | GridSkeleton | EmptyState | ProductGrid (ProductCard: photo on a mat or monogram) + Pagination
```

<!-- BUILD FLOW 01.05.15 — Phase 13 -->
## Phase 13 — Product pages, the local admin, and accessibility

**What we had before.** A single catalog page. Products could be changed only through the API, for example with the
`.http` examples.

**What we add.**
- **Pages:** a product page for each product, and a local admin to add, edit, and delete products, with a
  confirmation before deleting.
- **The write switch:** the storefront asks the server whether writes are on, and shows the admin only then.
- **A small router:** in-app links, Back and Forward with restored scroll positions, focus moved to the new page,
  and a title per page.
- **Accessible forms:** labels, hints, and errors wired for screen readers; an error summary; explained conflicts.

**Why.** The plan requires the detail and admin views. Every write path the API offers (validation, duplicate SKUs,
version conflicts, read-only mode) should be explained to a person in plain words, not just survived.

**Files in order**

| BUILD FLOW | File | Role |
|---|---|---|
| 13.01 | `backend/src/main/java/com/example/catalog/api/SettingsController.java` | `GET /api/v1/settings` |
| 13.02–13.03 | `frontend/src/lib/routes.js`, `components/Link.jsx` | Pages and addresses; in-app links |
| 13.04–13.06 | `frontend/src/hooks/useSettings.js`, `useProduct.js`, `usePageTitle.js` | Settings, one product, tab titles |
| 13.07–13.11 | `frontend/src/pages/CatalogPage.jsx`, `ProductPage.jsx`, `AdminPage.jsx`, `ProductFormPage.jsx`, `NotFoundPage.jsx` | The pages |
| 13.12–13.14 | `frontend/src/components/ConfirmDialog.jsx`, `FormField.jsx`, `lib/productForm.js` | Dialog, accessible field, the form's rules |
| 13.15–13.19 | `productForm.test.js`, `routes.test.jsx`, `test/fakeApi.js`, `ProductPage.test.jsx`, `AdminPages.test.jsx` | Tests and the shared fake backend |

Modified:
- **Backend:** `ProductApiIntegrationTest` (07.01: the settings in both modes).
- **Frontend core:**
  - `useUrlState.js` (12.13) is now the router's core.
  - `App.jsx` (12.29) is now the frame and router; the catalog moved to `CatalogPage`.
  - `products.js` (12.12) gained five calls and typed errors.
  - `useProductSearch.js` (12.15) remembers recent answers for Back.
- **Frontend components:**
  - `SiteHeader` (12.16) has the Admin link.
  - `SearchBox` (12.18) takes its own id and label.
  - `ProductGrid` and `ProductCard` (12.24, 12.25) make each card a link.
  - `catalog.js` (12.10) gained the monogram helper.
- **Styles and tests:** `app.css` (12.09.19–12.09.22); `setup.js` (12.30); `App.test.jsx` (12.32: the shared fake
  backend).
- **Docs:** D-054 to D-056; plan §4; `REQUEST_FLOWS.md` (READ-01, WRITE-01, WRITE-08); README; FILE_GUIDE;
  PROGRESS; LEARNING_LOG.

**Relevant syntax**
- *React*:
  - `useSyncExternalStore(subscribe, getSnapshot)` subscribes to state outside React (the address bar).
  - `key` resets a component: `key={product.version}` gives the form a fresh start.
  - A render prop (`children(props)`) lets `FormField` hand attributes to the control.
  - `useRef` gives direct access to a DOM element, here to move focus.
- *History API*: `pushState(state, '', url)` with a state object (the results address, a message);
  `history.state`; `history.back()`; `history.scrollRestoration = 'manual'`.
- *HTML and ARIA*:
  - links: `<a href>` with `preventDefault()` only for plain clicks;
  - native elements: `<dialog>` with `showModal()`, and `<output>` (implicit role "status");
  - forms: `aria-describedby`, `aria-invalid`, `fieldset`/`legend`;
  - tables: `<th scope="row">` and `<caption>`;
  - focus: `tabIndex={-1}` on elements that receive focus from code;
  - CSS: `:has(:focus-visible)`.
- *Testing*: `toHaveAccessibleDescription`, `toHaveFocus`, `within(...)`, `user.selectOptions`, a stubbed `fetch` that
  records every call; a pending promise to keep a request open.

**Commands we ran** (PowerShell)

| Command | Purpose |
|---|---|
| `.\mvnw.cmd -B -ntp clean verify` | The backend with the settings endpoint |
| `npm run lint`, `npm test` (several full runs), `npm run build` | Lint, tests, and a check for flaky tests |
| A deliberately broken `deleteProduct` call, then `npx vitest run src/pages/AdminPages.test.jsx` | Prove the delete test fails when deleting doesn't happen |
| `.\scripts\dev-services.ps1 up`; the jar with `dev`; `npm run dev` | The real stack, writes allowed |
| Browser scripts (Playwright API, headless Chromium) | The journeys, keyboard paths, phone layouts, screenshots |
| The jar with `--catalog.writes-enabled=false`; a direct `DELETE` from the page | Read-only mode, in the interface and on the server |

**Expected behavior.**
- **Navigation:** a card opens its product page, Back returns to the same results and scroll position, and focus moves
  to the new page.
- **Forms:** invalid input is explained before anything is sent, and the server's answers appear on the right field.
- **Conflicts and deletes:** conflicts are explained, not overwritten, and nothing is deleted without confirmation.
- **Read-only:** a read-only server shows no admin, and refuses writes anyway.

**Actual checks** (2026-10-31)

| Check | Result |
|---|---|
| Backend `clean verify` | 110 tests, 0 failures (two new: the settings with writes on and off) |
| `npm run lint` / `npm test` / `npm run build` | Clean (after the fixes below) / 39 passed in four full runs in a row / 259 KB JavaScript (80 KB gzipped), 19 KB CSS |
| Proving a test can fail | With the delete call disabled, "asks before deleting, deletes, and says so" failed; restored afterwards |
| Real stack: product page | Title "Ceramic Bud Vase — FORM"; focus on `<main>`; photo loaded with its alt text; "In stock: 25 available"; SKU shown |
| Real stack: back to results | Same address (`?category=HOME`), scroll position 500 → 500, the category still selected |
| Keyboard | Tab order: skip link, wordmark, Admin, search, categories. Enter on a card opened its page; the card showed the focus ring |
| Add a product | Empty submit: focus on the summary listing 6 problems, no request sent; a summary link focused its field; a SKU typed in lower case became capitals; "Saved “…”." |
| Edit and the cache | After changing the price, the product page showed ₹1,550 at once (the cached copy was evicted, Phase 10) |
| Version conflict | A second writer saved first: the form explained it, and "Load the latest version" showed their price (₹1,600) |
| Delete by keyboard | The dialog opened with focus on Cancel; Escape closed it and returned focus, with no request sent; Tab and Enter on "Delete product" sent one DELETE; the message received focus; the product page then said "This product doesn't exist." |
| Phone width (375 px) | Product page and form: no sideways scrolling. Admin: the table scrolls inside its frame (after a fix), and focusing an off-screen button scrolls it into view |
| Read-only server | No Admin link and no edit link; `/admin` and `/admin/products/new` explain that changes are off and show no form; a `DELETE` sent directly from the page got 403 `writes-disabled`, and the product still existed |
| Console | Only the deliberate 409 (conflict), 404 (after delete), and 403 (read-only check) |

Mistakes and findings on the way:
- **Lint found seven accessibility issues:**
  - an `alt` text that said "photo of";
  - four `role="status"` paragraphs, now `<output>`;
  - a scroll region with `tabIndex={0}`, now a `<section>` whose links and buttons are the keyboard path.
- **A test that passed alone failed in the full suite.** The page title is set by an effect, so the test now waits
  for it. Four full runs then passed in a row.
- **The admin table stretched the page on phones.** A visually hidden column header was positioned against the page
  instead of the scrolling frame. `position: relative` on the frame fixed it.
- **Links styled as buttons kept the underline,** which showed in the screenshots.
- **Header order:** CSS `order` would have made the visual order and the Tab order disagree, so the Admin link moved
  before the search box in the HTML instead.
- **Restoring the sabotaged file broke its special characters.** Windows PowerShell 5.1 read the UTF-8 file as
  Windows-1252, so writing it back double-encoded “ ” — …. This was noticed in the check output and reversed exactly
  (UTF-8 → Windows-1252 bytes); every frontend file was then scanned for the damage, and none remained.

**How the pieces connect**

```text
address bar ─► useUrlState (useSyncExternalStore) ─► App: matchRoute(pathname)
   "/"                       ─► CatalogPage ─► ProductCard <Link to=/products/42 state={from}>
   "/products/42"            ─► ProductPage ─► useProduct ─► GET /api/v1/products/42 ─► "Back to results" = history.back()
   "/admin"                  ─► AdminPage   ─► list (search) · Edit <Link> · Delete ─► ConfirmDialog ─► DELETE
   "/admin/products/new|42/edit" ─► ProductFormPage ─► validateProduct ─► POST | PUT {…, version} ─► navigate(/admin, flash)
useSettings ─► GET /api/v1/settings ─► writesEnabled ─► Admin link, edit link, admin pages (server still enforces 403)
navigate(): pushState + scrollY saved ─► useNavigationEffects: restore on Back, top + focus <main> on a new page
```
