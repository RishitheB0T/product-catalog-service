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
