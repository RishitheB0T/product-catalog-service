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
1. Run the app with the dev profile and open http://localhost:24081/actuator/health in a browser (the port moved
   from 8081 to 24081 in Phase 03). Find the disk-space path. Explain in one sentence why production hides it.
2. Temporarily change `show-details: never` to `always` in `application-prod.yml` and run `.\mvnw.cmd verify`.
   Which test fails, and why is that failure a good thing? Then undo the change.
3. Run the jar with `--spring.profiles.active=prod --server.port=24082`. Which log line proves the profile, and what
   is missing from the top of the log compared with a run without a profile?
4. Why does `mvnw` need Git's executable bit while `mvnw.cmd` does not?

<!-- BUILD FLOW 01.09.04 — Phase 03 -->
## Phase 03 — Containers for MySQL and Redis

**Concepts**
- **Image versus container.** An image is a read-only template (`mysql:8.4.11`). A container is a running instance
  of it, with its own process, network address, and file changes. Deleting a container does not delete the image.
- **Compose.** One YAML file describes containers that belong together. `up -d` starts them in the background and
  `down` removes them. The *project name* keeps one project's containers, networks, and volumes apart from another's.
- **Port publishing.** Containers sit on their own private network. `"127.0.0.1:24306:3306"` forwards this
  computer's port 24306 to port 3306 inside the container, on the loopback address only.
- **Loopback.** `127.0.0.1` (the name `localhost`) is the computer talking to itself; other devices cannot connect.
- **Volumes.** A container's files are disposable. A named volume is storage Docker keeps separately, so the
  database survives `down`; `down --volumes` deletes it.
- **Health checks.** A command Docker runs inside the container on a schedule. "Running" only means the process
  started; "healthy" means it answers correctly. `up --wait` waits for healthy.
- **Environment files.** `.env` keeps settings and secrets out of the YAML; `${NAME:?message}` refuses to start
  without them.
- **Database versus cache persistence.** MySQL must never lose data; this Redis may lose everything on a restart,
  because the cache can be rebuilt from MySQL. One has a volume; the other has persistence switched off.
- **Eviction.** With `maxmemory`, a full Redis evicts keys instead of rejecting writes; `allkeys-lru` removes the
  least recently used ones.
- **Least privilege.** The application's database user can only touch the `catalog` database.
- **Cryptographic randomness.** Passwords come from a cryptographic generator, and bytes that would skew the
  character distribution (modulo bias) are thrown away.
- **Ports on a shared machine.** Pick an unusual block, and check both what is listening and Windows' reserved ranges.
- **Test what you claim.** Restarting a *new* container proves nothing about persistence; restart the same one.

**Exercises**
1. Run `.\scripts\dev-services.ps1 status`, then the raw command it wraps:
   `docker compose --env-file infra\.env -f infra\compose.dev.yaml ps`. Why is the script still worth having?
2. Remove `127.0.0.1:` from the MySQL port mapping (don't commit it), run `up`, and check
   `Get-NetTCPConnection -LocalPort 24306 -State Listen`. What changed, and who could connect now? Restore it afterwards.
3. Why does the MySQL health check connect with `-h 127.0.0.1` inside the container, when the server runs right there?
4. Delete `infra/.env` but keep the volume, then run `up`. What happens to the passwords, and why does MySQL ignore
   the new ones? (Recover with `.\scripts\dev-services.ps1 reset -Force`, which deletes the database.)

<!-- BUILD FLOW 01.09.05 — Phase 04 -->
## Phase 04 — Schema, entity, repository

**Concepts**
- **ORM, JPA, Hibernate.** An object-relational mapper turns rows into objects and changes into SQL. JPA is the
  standard (the `jakarta.persistence` annotations); Hibernate is the implementation that does the work.
- **Entity and persistence context.** Within one transaction Hibernate keeps one managed object per row. It notices
  changed fields and writes them at *flush* time (dirty checking). `entityManager.clear()` forgets those objects, so
  the next read really goes to the database.
- **Migrations.** Versioned SQL files run once, in order, recorded in `flyway_schema_history` with a checksum. An
  applied migration is never edited; a change gets a new file. `ddl-auto=validate` makes Hibernate check the result.
- **Surrogate versus business key.** `id` is a meaningless number for joins and URLs; `sku` is the identifier
  people use, protected by a unique constraint.
- **Constraints in the database.** `NOT NULL`, `UNIQUE`, and `CHECK` hold for every client, including scripts and
  bugs. Named constraints produce readable errors.
- **Money.** `DECIMAL` and `BigDecimal` are exact; `DOUBLE` stores binary fractions and rounds.
- **Time.** An `Instant` is a moment in UTC. Store UTC and convert only for display. `DATETIME` avoids `TIMESTAMP`'s
  2038 limit.
- **Optimistic locking.** The *lost update* problem: two people edit the same product and the second save silently
  overwrites the first. `@Version` makes the second save fail instead, and the user can reload and retry.
- **Repositories.** Spring Data generates the implementation of an interface. Method names such as `findBySku`
  become queries; `Optional` says "may be absent".
- **Dependency injection.** Spring creates *beans* and passes them to the constructors that need them. Classes
  declare their dependencies instead of building them, which makes them easier to test and reconfigure.
- **Profiles for beans.** `@Profile("dev")` decides whether a bean exists at all, not only which settings apply.
- **Testcontainers.** Tests start a real MySQL in Docker; `@ServiceConnection` hands its address to Spring. A *slice*
  test (`@DataJpaTest`) starts only the persistence layer and rolls back each test.
- **Exception translation.** Spring converts database errors into its `DataAccessException` family, so code does not
  depend on driver-specific exceptions. How well a given error is classified depends on the path (JDBC or JPA).
- **Configuration from the environment.** `SPRING_DATASOURCE_URL` sets `spring.datasource.url`: production settings
  come from the server's environment, not from files in the jar.

**Exercises**
1. Look at `flyway_schema_history`. Then change only a comment in `V1__create_product_table.sql` (don't commit) and
   start the app. What does Flyway say, and why? Undo the change.
2. Use the README's SQL command to insert a product with price `-5`. Which constraint stops it?
3. In `ProductRepositoryTest`, what would `savesAndReloadsAProduct` prove if `entityManager.clear()` were removed?
4. Why does `rejectsAnUpdateBasedOnAnOutdatedVersion` change the row with `JdbcTemplate` before saving the entity?
5. Suppose `hibernate.jdbc.time_zone` were removed and the server ran in UTC while your laptop runs in IST. What
   would happen to timestamps written by one and read by the other?

<!-- BUILD FLOW 01.09.06 — Phase 05 -->
## Phase 05 — CRUD API

**Concepts**
- **Layers with one job each.** Controller: HTTP (parse, validate, status codes). Service: rules and the transaction.
  Repository: SQL. Each can be tested on its own.
- **DTOs and records.** Request and response records define the API contract. Fields a client must not set don't
  exist in the request type, which prevents "mass assignment".
- **Bean Validation.** Annotations such as `@NotBlank` and `@Digits` describe the rules; `@Valid` checks them before the
  controller runs and reports every broken field at once.
- **Transactions through a proxy.** `@Transactional` methods begin a transaction on entry, commit on a normal return,
  and roll back when an unchecked exception escapes. Calls within the same class bypass the proxy (self-invocation).
  `readOnly = true` skips dirty checking and marks the database transaction read-only.
- **Flush timing.** Hibernate normally writes at commit; `saveAndFlush` writes now, so database errors appear where
  the service can translate them.
- **HTTP status codes.** 200 OK, 201 Created with `Location`, 204 No Content, 400 Bad Request, 403 Forbidden,
  404 Not Found, 409 Conflict. PUT replaces the whole resource.
- **Problem Details (RFC 9457).** One standard error shape: `type`, `title`, `status`, `detail`, `instance`, plus
  extensions such as `errors`.
- **Check-then-act races.** "Exists? Then insert" can let two requests through; a unique key cannot be fooled. Let the
  database decide, then translate its answer.
- **Optimistic locking over HTTP.** The client sends the version it read; a mismatch means someone else saved first,
  so the server answers 409 and the user reloads instead of silently overwriting.
- **Fail-safe configuration.** Dangerous abilities (writes) are off unless explicitly enabled.
- **Test doubles and slices.** A Mockito mock replaces the service, so the web layer is tested quickly and in
  isolation; real-database tests prove what the mocks cannot (commits, rollbacks, constraints).
- **`AUTO_INCREMENT` gaps.** Failed inserts and rollbacks consume ids. Ids identify rows; they don't count them.

**Exercises**
1. Run the README's create–update–delete commands, then send the same `PUT` twice. What happens the second time, and
   why is that useful?
2. Remove `@Valid` from `createProduct` (don't commit) and post a negative price. Which layer rejects it now, with
   which status code? Restore `@Valid`.
3. Why does `ProductService.createProduct` call `saveAndFlush` instead of `save`?
4. Why can't the service simply call `findBySku` before inserting, and skip the unique constraint?
5. With the `prod` profile, send a `POST` whose body is invalid. Is the answer 400 or 403? Explain from `REQUEST_FLOWS.md`.

<!-- BUILD FLOW 01.09.07 — Phase 06 -->
## Phase 06 — Search

**Concepts**
- **Dynamic queries with Specifications.** One small predicate per filter, combined at runtime with AND. Only the
  filters that were sent become part of the WHERE clause, and every value is a bound parameter.
- **Bound parameters vs. string building.** A bound value can never change the structure of the SQL statement, which
  is why `x' OR '1'='1` in the search box is harmless. Sort keys cannot be bound parameters at all (they are part of
  the statement's structure), so they come from an allowlist instead.
- **Total ordering.** `ORDER BY price` alone leaves ties in an unspecified order, which can differ between two queries.
  Adding the unique `id` gives every row exactly one position, so pages never overlap or skip rows.
- **Offset pagination.** `LIMIT size OFFSET page × size`. Simple and allows jumping to any page, but MySQL reads and
  discards every skipped row, and pages shift when rows are inserted. Keyset pagination is the alternative for deep
  or endless lists.
- **Page queries and count queries.** A page needs the total for "page 2 of 5"; Spring Data runs a second `COUNT`
  query unless the page itself already reveals the total.
- **LIKE wildcards and escaping.** `%` (any run of characters) and `_` (any one character) in user text must be escaped,
  and the escape character itself must be escaped first.
- **Collations.** `utf8mb4_0900_ai_ci` is accent-insensitive (`ai`) and case-insensitive (`ci`), so equality and LIKE
  ignore both without `LOWER()`.
- **Data binding.** Spring builds a record from query parameters by name, converts text to the component types through
  `Converter`s, and reports values it cannot convert as field errors, just like broken validation rules.
- **Custom constraints.** A class-level annotation with a `ConstraintValidator` checks rules that span fields
  (min ≤ max) and can attach the error to a chosen field.
- **Compact constructors.** A record's compact constructor can normalize input (strip spaces) and fill defaults, so
  every instance is valid to use however it was created.

**Exercises**
1. Run `GET /api/v1/products?sort=price_asc&size=5` and then `?sort=price_asc&size=5&page=1`. Which products tie on
   price, and how does the order between them stay the same on both pages?
2. Explain why `ProductSort` lists the sort orders in code instead of reading the column name from `?sort=`. What
   could a client do if any attribute were allowed?
3. What would `?q=100%` find if `escapeLike` did not exist? And if it escaped `%` and `_` but forgot `!`?
4. Why does `ValidPriceRange` use `compareTo` and not `equals`? Try `new BigDecimal("1000").equals(new BigDecimal("1000.00"))`.
5. With 24 products and `size=10`, how many SQL statements run for page 0, page 2, and page 3? Turn on SQL logging and
   check your answer.
