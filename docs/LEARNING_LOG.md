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

<!-- BUILD FLOW 01.09.08 — Phase 07 -->
## Phase 07 — Full-stack tests

**Concepts**
- **The test pyramid.** Many fast, narrow tests (web slice with a test double, repository slice), fewer tests through
  the service on a real database, and a few end-to-end tests over real HTTP. Each level catches what the others
  cannot see; the top level is the slowest and is kept for the seams.
- **MockMvc vs. a real server.** MockMvc calls Spring MVC inside the test (no port, no socket). `RANDOM_PORT` starts
  Tomcat on a free port, so headers, unknown paths, percent-decoding, and truly simultaneous requests are real.
- **Committed test data.** Without a test transaction, rows stay in the shared database, so tests must never depend
  on the table being empty: unique SKUs and per-test name prefixes keep them independent.
- **Spring's test-context cache.** Test classes with identical configuration share one running application; each
  different configuration starts another one, with its own connection pool.
- **Races and invariants.** A race test cannot choose who wins, so it asserts what must hold for every interleaving:
  exactly one success, all others conflicts, one row.
- **Lost updates and optimistic locking.** When requests read the same version at the same moment, only the database
  can tell them apart: `UPDATE … WHERE version = ?` lets the first commit win and makes the others match no row.
- **Unique keys under concurrency.** Simultaneous inserts of the same key wait for the first one's lock; when it
  commits, the others fail with a duplicate-key error (1062).
- **Mass assignment, end to end.** Fields that the request record does not declare (`id`, `version`, timestamps) are
  ignored, so a client cannot set them.
- **`.http` files.** Plain-text HTTP requests that an editor can send; `###` separates requests, and named requests
  let later requests reuse earlier answers.

**Exercises**
1. In `ProductApiIntegrationTest`, why does `requestsTravelOverARealNetworkConnection` exist? What would silently
   change if someone added `@AutoConfigureMockMvc` to the test class?
2. The race test sends ten updates with version 0. Explain why the answer is one 200 and nine 409s whichever request
   arrives first, and which of the two version checks stopped the losers in the manual race.
3. Delete `@Version` from `Product` on a scratch branch (don't commit), run the update race, and explain the result.
4. Why do `ProductApiIntegrationTest.WithWritesEnabled` and `ProductSearchApiIntegrationTest` start only one
   application between them? Add one property to one of them and count the applications in the test log.
5. Open `backend/api-examples.http` in VS Code with the REST Client extension and send the requests in order. Then
   send "Create" twice in a row. What do you get the second time, and why?

<!-- BUILD FLOW 01.09.09 — Phase 08 -->
## Phase 08 — Measured query optimization

**Concepts**
- **Measure, then change, then measure again.** An index is chosen because a plan shows the work it removes, and kept
  only if the measurement after the change confirms it, with identical results.
- **Reproducible test data.** A seeded random generator with integer maths and a fixed reference date produces the
  same data everywhere; a fingerprint (a hash over all of it) proves it stayed the same.
- **`EXPLAIN` vs. `EXPLAIN ANALYZE`.** `EXPLAIN` shows the plan the optimizer intends, with estimates.
  `EXPLAIN ANALYZE` runs the query and adds the actual time and the actual rows of every step.
- **How a B-tree index helps.** It can narrow rows (seek to a value or range), deliver rows in order (so `LIMIT` can
  stop early), or answer from the index alone (a covering index). InnoDB appends the primary key to every secondary
  index.
- **Composite indexes.** Put equality columns first, then the range or sort column. The leftmost-prefix rule means
  `(category, price)` serves `category` alone, but never `price` alone.
- **Sort direction.** One index can be read forwards or backwards, but not "mixed": `price DESC, id ASC` needs a sort
  step.
- **Selectivity.** A condition that keeps 93 % of rows (in stock) is not worth an index. One that keeps 0.02 % is.
- **Leading wildcards.** `LIKE '%term%'` cannot seek in a B-tree: the matching values are scattered through the index.
- **The optimizer can bet wrong.** With `ORDER BY … LIMIT`, walking an ordered index pays off when matches are common
  and costs a full walk when they are rare. Measure the rare case too.
- **Counts and offsets.** `COUNT` must visit every matching row, and `OFFSET n` must produce n rows only to skip them.
  Indexes help neither much.
- **Costs of an index.** Space (it depends on how the index was built), extra work on every write, and an online build
  when it is added to a live table.
- **Migrations are history.** A schema change after V1 is a new migration (V2). One DDL statement is atomic in
  MySQL 8; several are not.

**Exercises**
1. In `before-v2.txt`, find the step that reads all 100,000 rows in category-newest. Which step in `after-v2.txt`
   replaced it, and how many rows does that step read?
2. Why does `ix_product_category_created_at` not help the home page? Use the leftmost-prefix rule.
3. Explain from the plans why `?q=linen` became 73× faster while `?q=zebra` became 4× slower. What would you measure
   before deciding to fix it?
4. Why is `price DESC, id DESC` a single backward read of `ix_product_category_price`? What would change if the
   tie-breaker were `id ASC`?
5. Run `.\scripts\measure-queries.ps1 -Label my-run -Baseline after-v2` (the bench database must be loaded). Are the
   fingerprints identical? Are the times? What does that tell you about comparing single timings? Delete
   `benchmarks/query-plans/my-run.txt` afterwards; it is not part of the recorded evidence.

<!-- BUILD FLOW 01.09.10 — Phase 09 -->
## Phase 09 — Redis cache for product details

**Concepts**
- **Cache-aside.** The application looks in the cache first and fills it itself after a database read; the database
  stays the source of truth.
- **What to cache.** Data read often, changed rarely, and addressable by one key (a product by id). Search results
  depend on many rows and parameters, so invalidating them correctly is hard.
- **TTL.** Time to live: Redis deletes a key by itself. It is the upper bound on how long an outdated copy can survive,
  and the trade-off between hit rate and freshness.
- **Key design.** A namespace (`catalog:`), a format version (`v1`), and the id, so keys never collide and a format
  change never misreads old values.
- **Serialization.** JSON written by the same mapper as the API: readable, language-neutral, and identical to the
  response. Java serialization would be binary and fragile.
- **Negative caching.** Storing "not found" saves database reads but can hide a product created a moment later; not
  done here.
- **Transaction scope.** A transaction should cover only database work. Calling Redis while holding a database
  connection wastes a scarce resource.
- **Graceful degradation.** A cache failure becomes a slower answer from the database, never an error, and is counted
  so it is noticed. Only expected failure types are caught.
- **Health of dependencies.** An optional dependency (a cache) must not make the service report itself DOWN.
- **Metrics.** Counters with tags (`result=hit|miss|error`) give the hit rate and reveal failures.

**Exercises**
1. Start the dev backend with `--catalog.cache.enabled=true`, read product 7, change its price with `PUT`, and read it
   again within 60 seconds. What do you see, and which Phase 10 change fixes it?
2. Why is `@Transactional` no longer on `getProduct`? What would a cache hit cost if it were?
3. Run `redis-cli TTL catalog:v1:product:7` a few times. What happens when it reaches 0, and what does the next read do?
4. Put "not json" into a key with `redis-cli SET`, read the product through the API, and look at the key again. Which
   counter changed?
5. Why would including Redis in the health check be dangerous behind a load balancer?

<!-- BUILD FLOW 01.09.11 — Phase 10 -->
## Phase 10 — Eviction after commit, and Redis failures

**Concepts**
- **Invalidate after the commit.** Clearing a cached copy before the commit leaves a gap in which another request
  stores the old, still-committed row again. After a rollback there is nothing to clear.
- **Delete, don't update, on writes.** Concurrent writers could store values in the opposite order from their commits;
  a delete is idempotent and lets the next read fetch what is committed.
- **Transactional events.** Publish "what happened" inside the transaction; Spring delivers it in a chosen phase
  (`AFTER_COMMIT`) only if the transaction got there.
- **Timeouts as a design decision.** A client default of 60 seconds turns a hung dependency into a hung service. A
  timeout well above normal latency and well below user patience bounds the damage.
- **Graceful degradation, measured.** With Redis stopped the API stayed correct, just slower (about 430 ms per read).
- **Never misreport a committed write.** Work after the commit (eviction) may fail; the write still succeeded.
- **Client timeouts do not cancel server work.** A command the client gave up on may still run later.
- **The stale-read race.** A slow reader can store an old copy after a writer's eviction; the TTL bounds it. Stronger
  fixes: versioned compare-and-set, delayed double eviction, change-data-capture.
- **Cache stampede.** Many requests missing the same key at once; coalescing, early refresh, or TTL jitter help.
- **Testing failures for real.** Pausing a real container proves timeouts and fallbacks; a mock would only prove the
  code calls what it was told to.

**Exercises**
1. Move the eviction into `updateProduct` itself, before the method returns (don't commit). Describe the interleaving
   in which another request re-caches the old product.
2. In `ProductCacheOutageTest`, why does the read take about 400 ms and not 200 ms?
3. Set `spring.data.redis.timeout` back to its default on a scratch branch and run `ProductCacheOutageTest`. What
   happens, and how long does it take?
4. Explain why `aSlowReaderCanStillStoreAnOldCopyAfterAWrite` passes, and name one way to make the stale copy
   impossible.
5. With the dev stack running, stop Redis (`docker compose … stop redis`), update a product, start Redis again, and
   read the product. Is it outdated? Why not, here, and when would it be?

<!-- BUILD FLOW 01.09.12 — Phase 11 -->
## Phase 11 — Benchmarks and evidence

**Concepts**
- **Measure the whole request.** A query that takes 0.1 ms inside MySQL can still be a 30 ms request. The request
  also includes the network, the JVM, JSON, a second query (the count), and waiting for a CPU.
- **Closed and open models.** In a closed model, a fixed number of users each send the next request when the last
  one answers. In an open model, requests arrive at a fixed rate, however slow the answers are. A closed model
  sends fewer requests when the server slows down, so it can hide how bad latency would get under real arrivals.
  This effect is called *coordinated omission*.
- **Little's law.** Users = throughput × time per request. In a closed model, higher throughput and lower latency are
  the same result, and the law is a quick check that the numbers are consistent.
- **Warm-up, fresh start, several trials.** Throw away the first seconds (JIT compilation, empty buffer pools), and
  report the median with the range rather than one lucky run.
- **Percentiles.** The median shows a typical request and p95 the slow tail; a mean hides both.
- **Cold and warm caches.** A hit skips the database; a miss costs more than no cache at all. The hit ratio and the
  number of reads per TTL decide whether the cache pays off.
- **A speed-up only helps as much as the share it speeds up.** The indexes made page queries up to 483 times faster,
  but the counts remained, so whole searches got only 3–6 times faster (Amdahl's law).
- **Valid before fast.** Decide whether a run counts by facts about the run itself, never by its results. Examples are
  its real length, failed connections, sleep, and whether a cold pass was really cold. Record runs that were thrown
  away, and why.
- **A silent failure can look like a great result.** A cache "warmed" by a failed delete made the cold numbers look
  excellent. Checking the counters, not just the timings, caught it.
- **The load generator is part of the system.** On one laptop, k6, Docker's network forwarding, and the server
  compete for the same CPU. Here a property of that path, connection stalls, nearly ended up in the numbers.

**Exercises**
1. From the report, use Little's law to predict the cache-off throughput with 20 users if latency stayed the same.
   Then explain why it wouldn't stay the same on this laptop.
2. Run `.\scripts\run-benchmarks.ps1 -SkipIndexComparison -Trials 1 -Vus 1` and compare the medians with the
   10-user results. How much of the 10-user latency was waiting?
3. Using your own run's medians, compute how many reads per minute a product needs before the cache pays off.
4. Why did all 1,162 warm-cache misses fall in trial 2? Change the protocol (not the TTL) so that no warm trial sees
   an expiry.
5. Which single change would make the search mix fastest next, and how would you measure it before and after?
6. Remove `--server.tomcat.max-keep-alive-requests=-1` from the script on a scratch branch and run it. Which check
   reacts, if any, and what does the report say?

Exercises 2 and 6 overwrite `benchmarks/results/`. Afterwards, `git restore benchmarks/results` brings back the
published files.

<!-- BUILD FLOW 01.09.13 — Phase 12 -->
## Phase 12 — The storefront catalog

**Concepts**
- **Components, props, and state.** A component is a function from props and state to markup. React re-renders it
  when either changes; nothing is changed by hand in the DOM.
- **One source of truth.** The search lives in the address bar. The API query, the count, and the chips are
  computed from it on every render instead of being copied into more state that could drift.
- **The URL as state.** `pushState` adds a history entry and `replaceState` overwrites one; `popstate` reports Back
  and Forward. A shared or refreshed link shows the same results.
- **Effects and cleanup.** An effect runs after rendering. Its cleanup runs before the next run and on unmount. That
  is where a request is aborted and a listener removed.
- **Race conditions in the browser.** Without cancellation, a slow old answer can arrive after a newer one and
  overwrite it. `AbortController` cancels the old request, so the last search always wins.
- **Debouncing.** Wait for a pause before acting, so typing "linen" sends one request, not five.
- **Derived loading.** Comparing the query a result answers with the current query tells whether a newer request is
  still loading. No separate "loading" flag can get out of step.
- **Errors as sentences.** Problem Details carry machine-readable fields. The UI turns them into messages a person
  understands, and always offers a way forward.
- **Accessibility basics:**
  - labels for every field, `fieldset` and `legend` for groups;
  - `aria-pressed`, `aria-current`, `aria-live`, and `aria-busy`;
  - a skip link, and native `<dialog>`, `<search>`, and `<details>`.
- **Responsive, mobile-first CSS.** Base rules for phones, then `min-width` media queries for wider screens. Design
  tokens keep colours and spacing consistent.
- **Licensing.** "Free to use" means a specific license with specific duties (credit, share-alike). Record the source
  and license of every asset, and never present another brand's product as your own.

**Exercises**
1. Type quickly in the search box with the browser's network tab open. How many requests are sent? Change the pause
   in `SearchBox` to 0 and compare.
2. Remove the cleanup (`return () => controller.abort()`) from `useProductSearch`. Throttle the network in the
   browser's developer tools, click two categories quickly, and explain what can go wrong.
3. Why does `update` return to page 1 for every change except a page change? Find a case where keeping the page
   would show something wrong.
4. In `SearchBox`, why does the comparison with `sent` happen during rendering instead of in an effect? What would
   an effect show on screen for a moment?
5. Open the filter drawer with the keyboard only. Where is focus, how do you close it, and where does focus go
   afterwards? Which browser feature does that work?
6. Add a photo for one more product by following `docs/IMAGE_CREDITS.md`. Which licenses would you reject, and why?
