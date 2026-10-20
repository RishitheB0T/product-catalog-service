<!--
BUILD FLOW 01.06 — docs/FILE_GUIDE.md
First introduced: Phase 01
Purpose: One row for every tracked file: its BUILD FLOW number, exact path, purpose,
         dependencies, caller/consumer, and when to read it.
Depends on: The list of tracked files (`git ls-files`) and each file's BUILD FLOW header.
Read by: Anyone meeting an unfamiliar file, and the pre-commit check that nothing is undocumented.
Runtime role: None. Documentation only.
-->

# File Guide — every tracked file

**Rules.** Every file that `git ls-files` lists has a row here. Numbers come from each file's BUILD FLOW header.
Generated files are marked **(generated)** and are never edited by hand to add comments; their explanation lives
here instead. Binary and generated internals are grouped rather than listed line by line.

<!-- BUILD FLOW 01.06.01 — Repository root -->
## Repository root

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 01.02 | `.gitignore` | Keeps secrets, build output, dependencies, logs, and editor clutter out of Git | — | Git (`status`, `add`, `commit`) | Before adding tooling that creates new files |
| 01.03 | `.gitattributes` | LF line endings in the repository; CRLF for Windows scripts; binary-file rules | — | Git (checkout and commit) | When a script fails because of its line endings |
| 01.04 | `README.md` | Honest front page: status, architecture, prerequisites, documentation map | `docs/PROJECT_PLAN.md`, `docs/PROGRESS.md` | GitHub visitors, interviewers | First |

<!-- BUILD FLOW 01.06.02 — docs/ -->
## docs/

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 01.01 | `docs/PROJECT_PLAN.md` | Canonical plan: working rules, requirements, version baseline, 16 phases | The project requirements (restated in full) | Every other document links to it | Before each phase |
| 01.05 | `docs/BUILD_GUIDE.md` | The project in construction order, phase by phase | Plan, File Guide | Anyone studying the build | To study the build in order |
| 01.06 | `docs/FILE_GUIDE.md` | This table | `git ls-files`, file headers | Readers, the pre-commit check | When you meet an unfamiliar file |
| 01.07 | `docs/REQUEST_FLOWS.md` | Runtime step IDs (SEARCH, READ, WRITE); planned until Phase 05 | Plan §7 and §9 | Readers, code comments | To understand what happens during a request |
| 01.08 | `docs/DECISIONS.md` | Important choices, the alternatives, and the trade-offs | Plan | Readers, interview preparation | When asking "why this and not that?" |
| 01.09 | `docs/LEARNING_LOG.md` | Concepts learned and small exercises for each phase | Build Guide | Readers | After each phase |
| 01.10 | `docs/PROGRESS.md` | Current phase, last commit, checks, blockers, next action | All docs | Read first when resuming work | Whenever work resumes |

<!-- BUILD FLOW 01.06.03 — Generated files -->
## Generated files

None yet. Phase 02 adds the Maven Wrapper files and Phase 12 adds `package-lock.json`. They will be listed here,
marked **(generated)**.
