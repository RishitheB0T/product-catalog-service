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
| Current phase | **01 — complete** |
| Next phase | 02 — Maven/Spring Boot skeleton, profiles, basic health check |
| Last phase commit | Phase 01, `docs: define catalog architecture and phased build plan`. A commit cannot contain its own hash, so it is recorded here at the start of Phase 02. Until then, run `git log --oneline -1` |
| Branch | `main`, local only (no GitHub remote yet) |
| Last updated | 2026-10-20 |

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

<!-- BUILD FLOW 01.10.03 — Environment -->
## Environment (inspected 2026-10-20)

| Item | Found |
|---|---|
| Operating system | Windows 11 Home, 64-bit (build 26200); Windows PowerShell 5.1 |
| Hardware | 4 cores / 8 threads, 15.8 GB RAM. About 1 GB was free during inspection, with browsers holding much of it. Docker's Linux VM: 8 CPUs, about 7.6 GiB |
| Git | 2.55.0, with Git Credential Manager configured and a Git identity set |
| JDK | Microsoft Build of OpenJDK 21.0.12.1; `JAVA_HOME` set |
| Docker | Engine 29.6.2 (WSL 2 backend) and Compose v5.3.1; the engine was running |
| Node.js / npm | 26.8.1 ("Current"; becomes LTS on 2026-10-28) / 11.19.0 |
| Not installed | Maven (not needed; the wrapper downloads it), GitHub CLI (optional), k6 (planned to run from Docker), AWS CLI (decided in Phase 15) |

<!-- BUILD FLOW 01.10.04 — Blockers and pending items -->
## Blockers and pending items

| Item | Needed by | Status |
|---|---|---|
| GitHub repository and sign-in | Before Phase 14 (CI); Phase 16 (history check) | Pending. Create the repository and sign in at the GitHub step |
| AWS account access and approval of instance, region, and cost | Phase 15 | Pending |
| Anything blocking Phase 02 | — | None |

<!-- BUILD FLOW 01.10.05 — Next action -->
## Next action

Next: start Phase 02:
1. Record Phase 01's commit hash in "Current state" above.
2. Confirm the Java base package (default `com.example.catalog`).
3. Generate `backend/` with Spring Initializr (Spring Boot 4.1.1, Java 21, Maven) with Web MVC, Validation, and
   Actuator only. Pin the wrapper to Maven 3.10.0, and add profiles and a health-check test.
4. Run `.\mvnw.cmd test`, start the application, and call `/actuator/health`.
5. Commit `chore(backend): bootstrap Spring Boot application`.
