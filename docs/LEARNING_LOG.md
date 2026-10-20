<!--
BUILD FLOW 01.09 — docs/LEARNING_LOG.md
First introduced: Phase 01
Purpose: The concepts learned in each phase, in plain English, with small practical exercises.
Depends on: docs/BUILD_GUIDE.md (what was built in each phase).
Read by: Anyone reviewing the concepts, after each phase and before interviews.
Runtime role: None. Documentation only.
-->

# Learning Log

Concepts in the order we met them, each with small practice exercises.

<!-- BUILD FLOW 01.09.01 — Phase 01 -->
## Phase 01 — Planning the build

**Concepts**
- **Layered architecture.** The controller handles HTTP (what came in, what goes out). The service applies rules
  and owns transactions. The repository reads and writes the database. Each layer only calls the one below it, so
  each can be understood and tested on its own.
- **DTO (Data Transfer Object).** A small class shaped for the API, not for the database row. It keeps internal
  fields and JPA behavior out of the JSON that clients see.
- **Source of truth versus cache.** MySQL holds the real data. Redis keeps short-lived copies so common reads are
  faster. A copy can be briefly out of date, which is why it has a TTL (time to live) after which Redis deletes it.
- **Cache-aside.** The application checks the cache, falls back to the database on a miss, then fills the cache.
  The cache never talks to the database by itself.
- **LTS versus Current releases.** LTS (long-term support) versions receive fixes for years. "Current" releases are
  newer but are replaced sooner. We prefer LTS for anything we deploy: Java 21, MySQL 8.4, and Node LTS for builds.
- **Pinning and lockfiles.** Pinning means naming an exact version (`mysql:8.4.11`, not `mysql:latest`). A lockfile
  records the exact version of every dependency, including indirect ones, so a build next month installs the same code.
- **Git basics.** A repository records snapshots called commits. `git add` stages exactly what goes into the next
  snapshot, and `git commit` records it with your name, the time, and a message. Commits stay on this computer until
  they are pushed.
- **Line endings.** Windows ends lines with two invisible characters, CR and LF. Linux uses LF only. A Linux shell
  treats a stray CR as part of the command, so the script breaks. `.gitattributes` makes Git store LF.
- **.gitignore.** Patterns for files Git should never track: secrets (`.env`), build output (`target/`), and
  downloaded dependencies (`node_modules/`).
- **Read tool output carefully.** `git check-ignore -v` prints the rule that *matched* a path, and that includes
  `!` rules, which re-include a file. So `.env.example` appeared in the output even though it is **not** ignored.
  Without `-v`, the exit code answers the question: 0 means ignored, 1 means not ignored. A check is only useful if
  you know exactly what its output means.

**Exercises**
1. Run `git log --stat -1` and match each file in the first commit to its BUILD FLOW number in `docs/FILE_GUIDE.md`.
2. Run `git check-ignore -v .env` and then `git check-ignore -v .env.example`. Use `.gitignore` to explain why the
   results differ.
3. Run `git check-attr eol -- deploy.sh run.ps1` (the files don't need to exist). Which line ending does each one
   get, and why would it matter on EC2?
4. In your own words: why is `mysql:latest` a bad choice for a project you want to rebuild exactly a year from now?

<!-- BUILD FLOW 01.09.02 — Between Phases 01 and 02: GitHub -->
## Between Phases 01 and 02 — GitHub

**Concepts**
- **Remote.** A named address of another copy of the repository. `origin` is the conventional name for the main one.
- **Push and upstream.** `git push` uploads the commits the remote doesn't have yet. `-u` (upstream) remembers which
  remote branch your local branch follows, so `git status` can say "ahead of 'origin/main' by 1 commit" and a
  plain `git push` knows where to go.
- **Device-flow sign-in.** The terminal shows a short code, and you approve it in a browser where you are already
  signed in. The terminal never sees your password, and the token it receives is kept by the operating system's
  credential store.
- **Token scopes.** A token can do only what its scopes allow. `repo` allows repository work. Pushing GitHub
  Actions workflow files needs an extra `workflow` scope (Phase 14).
- **Verifying a push.** "The command printed no error" is weaker evidence than asking GitHub which commit its
  `main` points to and comparing hashes.

**Exercises**
1. Make a commit but don't push it, then run `git status`. What does "ahead of 'origin/main' by 1 commit" mean?
2. `git log --oneline origin/main -3` shows your *local copy* of what GitHub had at your last push or fetch.
   Why could it differ from GitHub right now? (Hint: `git fetch`.)

<!-- BUILD FLOW 01.09.03 — Phase 02 -->
## Phase 02 — Spring Boot skeleton

**Concepts**
- **Maven and the POM.** Maven builds Java projects. `pom.xml` names the project (coordinates), lists libraries
  (dependencies), and configures build steps (plugins). Running a lifecycle phase also runs every phase before it:
  `verify` includes compile, test, and package.
- **Parent POM and bill of materials.** `spring-boot-starter-parent` supplies one tested version for each library,
  so our dependencies don't state versions. Changing the parent's version upgrades them together.
- **Starters.** A starter is one dependency that brings a library plus Spring Boot's automatic setup for it.
- **Maven Wrapper.** `mvnw.cmd` / `mvnw` download the exact Maven version the project pins, so nobody installs Maven
  by hand. Our SHA-256 pin makes the wrapper reject a tampered download.
- **Auto-configuration and the application context.** At startup Spring Boot looks at the classpath and configures
  what it finds (Tomcat, Spring MVC, Actuator). The application context is the container holding the objects it
  creates, called *beans*.
- **Component scanning.** Spring looks for annotated classes in the main class's package and its sub-packages, which
  is why all our classes live under `com.example.catalog`.
- **Embedded server and fat jar.** Tomcat runs inside our application, not the other way round. `java -jar` starts
  everything from one file that contains our classes plus all libraries.
- **Profiles.** Named sets of configuration. `application-dev.yml` overrides `application.yml` only when `dev` is
  active. Command-line options such as `--server.port=8082` override both.
- **Actuator and health.** Operational endpoints. `/actuator/health` combines health indicators into UP or DOWN
  (HTTP 200 or 503). *Liveness* means "the process is alive"; *readiness* means "it can take traffic now". Details
  can reveal internals (we saw a real folder path), so production hides them.
- **Tests.** `@Test` marks a test method. `@Nested` groups tests in an inner class. `@SpringBootTest(webEnvironment =
  RANDOM_PORT)` starts the real app on a free port, `@LocalServerPort` injects that port, and `@ActiveProfiles`
  selects a profile. AssertJ reads like a sentence: `assertThat(x).isEqualTo(y)`.
- **A red test is information.** Our first run failed because Spring Boot 4 changed the health body. We checked
  the docs, decided the new behavior was fine, and changed the test to express the real intent.
- **Check the artifact, not just the tests.** All tests passed while the jar was missing its configuration files.
  Running the jar the way a server would is what exposed it. Build what you ship from a clean state.
- **Quoting in PowerShell.** `"-Dspring-boot.run.profiles=dev"` needs quotes; YAML needs quotes around `"off"`.

**Exercises**
1. Run the app with the dev profile and open http://localhost:8081/actuator/health in a browser. Find the
   disk-space path. Explain in one sentence why production hides it.
2. Temporarily change `show-details: never` to `always` in `application-prod.yml` and run `.\mvnw.cmd verify`.
   Which test fails, and why is that failure a good thing? Then undo the change.
3. Run the jar with `--spring.profiles.active=prod --server.port=8082`. Which log line proves the profile, and what
   is missing from the top of the log compared with a run without a profile?
4. Why does `mvnw` need Git's executable bit while `mvnw.cmd` does not?
