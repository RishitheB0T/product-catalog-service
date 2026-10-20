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
| Current phase | **02 — complete** |
| Next phase | 03 — local MySQL and Redis with Docker Compose, plus environment examples |
| Branch | `main`, tracking `origin/main` |
| Repository | https://github.com/RishitheB0T/product-catalog-service (public) |
| Last updated | 2026-10-20 |

**Commits so far** (each commit records the hash of the one before it, because a commit cannot contain its own hash)

| Step | Commit | Message | On GitHub |
|---|---|---|---|
| Phase 01 | `5be2fc3` | `docs: define catalog architecture and phased build plan` | Pushed and verified |
| Between 01 and 02 | `225433f` | `docs: connect GitHub remote` | Pushed and verified |
| Phase 02 | Recorded at the start of Phase 03 | `chore(backend): bootstrap Spring Boot application` | Pushed and verified when committed |

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

<!-- BUILD FLOW 01.10.03 — Environment -->
## Environment (inspected 2026-10-20)

| Item | Found |
|---|---|
| Operating system | Windows 11 Home, 64-bit (build 26200); Windows PowerShell 5.1 |
| Hardware | 4 cores / 8 threads, 15.8 GB RAM. About 1 GB was free during inspection, with browsers holding much of it. Docker's Linux VM: 8 CPUs, about 7.6 GiB |
| Git | 2.55.0, with Git Credential Manager 2.9.1 configured (GitHub account `RishitheB0T`) and a Git identity set |
| GitHub CLI | 2.102.0, installed with winget on 2026-10-20 and signed in as `RishitheB0T` (scopes `repo`, `read:org`, `gist`) |
| JDK | Microsoft Build of OpenJDK 21.0.12.1; `JAVA_HOME` set |
| Docker | Engine 29.6.2 (WSL 2 backend) and Compose v5.3.1; the engine was running |
| Node.js / npm | 26.8.1 ("Current"; becomes LTS on 2026-10-28) / 11.19.0 |
| Maven | 3.10.0 through the wrapper (downloaded to the user's `.m2` folder on 2026-10-20; checksum-verified) |
| Ports | 8080 is occupied by another program, so the `dev` profile uses 8081 |
| Not installed | k6 (planned to run from Docker), AWS CLI (decided in Phase 15) |

<!-- BUILD FLOW 01.10.04 — Blockers and pending items -->
## Blockers and pending items

| Item | Needed by | Status |
|---|---|---|
| GitHub repository and sign-in | — | Done on 2026-10-20: public repository created with the GitHub CLI; pushes verified |
| `workflow` token scope for pushing CI files | Phase 14 | Pending. The current tokens are not known to include `workflow`, which GitHub requires for pushing files under `.github/workflows/`. Plan: `gh auth refresh -s workflow` when needed |
| AWS account access and approval of instance, region, and cost | Phase 15 | Pending |
| Anything blocking Phase 02 | — | None |

<!-- BUILD FLOW 01.10.05 — Next action -->
## Next action

Next:
1. Record Phase 02's commit hash in the commits table above.
2. Phase 03: check that host ports 3306 and 6379 are free; re-verify the `mysql:8.4.x` and `redis:8.10.x` patch tags;
   write `infra/` Compose services (pinned images, named MySQL volume, health checks, ports bound to `127.0.0.1`),
   `.env.example` with placeholders, and small PowerShell helpers.
3. Verify: containers healthy, `SELECT VERSION()`, Redis `PING`, ports bound to `127.0.0.1` only, `.env` ignored.
4. Commit `chore(infra): add local MySQL and Redis services`, push, and verify on GitHub.
