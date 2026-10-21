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

<!-- BUILD FLOW 01.06.04 — backend/ (added in Phase 02) -->
## backend/

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 02.01 | `backend/pom.xml` | Maven build: Spring Boot 4.1.1 parent, Java 21, the starters (Web MVC, Validation, Actuator; since Phase 04 also Data JPA, Flyway with its MySQL module, the MySQL driver, and Testcontainers for tests), runnable-jar packaging; since Phase 05 Mockito runs as a Java agent in tests | Maven Wrapper, Maven Central | `mvnw.cmd` / `mvnw`; CI and Docker from Phase 14 | When adding a dependency or changing the build |
| 02.02 | `backend/mvnw` | **(generated)** Maven Wrapper script for Linux and macOS (CI, Docker, EC2): downloads the pinned Maven, checks its SHA-256, runs it. Stored with LF endings and the executable bit | `backend/.mvn/wrapper/maven-wrapper.properties` | Linux builds from Phase 14 | Only if the build tool itself misbehaves |
| 02.03 | `backend/mvnw.cmd` | **(generated)** The same wrapper for Windows, checked out with CRLF endings | `backend/.mvn/wrapper/maven-wrapper.properties` | Windows development machines | Only if the build tool itself misbehaves |
| 02.04 | `backend/.mvn/wrapper/maven-wrapper.properties` | **(generated, plus one setting)** Pins Maven 3.10.0 and wrapper 3.3.4. We added `distributionSha256Sum`, verified against Apache's published SHA-512 | — | `mvnw`, `mvnw.cmd` | When upgrading Maven: change the URL and the checksum together |
| 02.05 | `backend/src/main/java/com/example/catalog/CatalogApplication.java` | Entry point that starts Spring Boot. Its package is where component scanning starts; since Phase 05 it also scans for `@ConfigurationProperties` records | Spring Boot auto-configuration, the YAML files | The JVM, `spring-boot:run`, `@SpringBootTest` | The first Java file to read |
| 02.06 | `backend/src/main/resources/application.yml` | Base configuration with safe defaults: app name, Actuator exposure limited to health, no health details; since Phase 04 JPA settings (`ddl-auto=validate`, Open Session in View off, UTC) and the Flyway location; since Phase 05 `catalog.writes-enabled: false` | — | Spring Boot at startup | Before changing any setting |
| 02.07 | `backend/src/main/resources/application-dev.yml` | Dev profile: listens on `127.0.0.1:24081` (since Phase 03), full health details, DEBUG logs for our package; since Phase 04 the MySQL connection, reading the password from `infra/.env`; since Phase 05 writes enabled | `application.yml`, `infra/.env` | Local runs with the `dev` profile | When running locally |
| 02.08 | `backend/src/main/resources/application-prod.yml` | Prod profile: locked-down Actuator settings pinned, banner off; since Phase 05 writes explicitly disabled (read-only) | `application.yml` | Docker and EC2 (Phases 14–15) | Before deploying |
| 02.09 | `backend/src/test/java/com/example/catalog/HealthEndpointTest.java` | Real-HTTP tests of health, probes, and hidden endpoints under the base, dev, and prod configurations (each with the shared MySQL test container since Phase 04) | 02.05–02.08, 04.07, JUnit 6, AssertJ, Spring Boot test support | Maven Surefire (`mvnw verify`) | To see how profiles change behavior |
| 04.01 | `backend/src/main/resources/db/migration/V1__create_product_table.sql` | Flyway migration creating the `product` table: columns, primary key, unique SKU, six named CHECK constraints, UTC timestamp defaults, version column. No search indexes yet (Phase 08) | MySQL 8.4 | Flyway at startup and in tests | To understand the data model and its rules |
| 04.02 | `backend/src/main/java/com/example/catalog/product/Category.java` | The six allowed categories as a Java enum (mirrors `ck_product_category`) | — | `Product`; the API from Phase 05 | When adding a category (enum plus a new migration) |
| 04.03 | `backend/src/main/java/com/example/catalog/product/Product.java` | JPA entity for the `product` table: mappings, `@Version` optimistic locking, UTC timestamps set by lifecycle callbacks | 04.01, 04.02 | `ProductRepository`; the service from Phase 05 | Before touching persistence code |
| 04.04 | `backend/src/main/java/com/example/catalog/product/ProductRepository.java` | Spring Data repository: CRUD methods generated by Spring, plus `findBySku` | 04.03 | Tests; `ProductService` from Phase 05 | To see how the app talks to MySQL |
| 04.05 | `backend/src/main/resources/db/demo/demo-products.sql` | 24 demo products (four per category, two out of stock); not a migration | 04.01 | `DemoDataLoader` | When changing the demo catalog |
| 04.06 | `backend/src/main/java/com/example/catalog/demo/DemoDataLoader.java` | Dev-only bean that loads the demo SQL once, into an empty table; first use of constructor injection | 04.05, DataSource, JdbcTemplate | Spring Boot at startup with the `dev` profile | To learn dependency injection and profiles |
| 04.07 | `backend/src/test/java/com/example/catalog/TestcontainersConfiguration.java` | One shared MySQL 8.4.11 container for the whole test run, connected through `@ServiceConnection` | Docker, Testcontainers 2 | Every test that starts Spring with a database | When a test needs a database |
| 04.08 | `backend/src/test/java/com/example/catalog/product/ProductRepositoryTest.java` | Persistence tests on real MySQL: round trip, UTC storage, duplicate SKU, six CHECK constraints, JPA error type, optimistic locking | 04.01–04.04, 04.07 | Maven Surefire | To see what the database guarantees |
| 04.09 | `backend/src/test/java/com/example/catalog/demo/DemoDataLoaderTest.java` | Demo data loads only with `dev`, only into an empty table, never twice | 04.05–04.07 | Maven Surefire | To see how profile-specific beans are tested |
| 05.01 | `backend/src/main/java/com/example/catalog/config/CatalogProperties.java` | Typed `catalog.*` settings; the `writesEnabled` switch | The YAML files | `ProductService` | To see how settings become Java values |
| 05.02 | `backend/src/main/java/com/example/catalog/product/ProductCreateRequest.java` | POST body record with Bean Validation rules mirroring the database constraints | 04.02 | `ProductController`, `ProductMapper` | Before changing what clients may send |
| 05.03 | `backend/src/main/java/com/example/catalog/product/ProductUpdateRequest.java` | PUT body record: the complete product plus the version last read | 04.02 | `ProductController`, `ProductService`, `ProductMapper` | To understand optimistic locking in the API |
| 05.04 | `backend/src/main/java/com/example/catalog/product/ProductResponse.java` | Response record: the public shape of a product, including `inStock` and `version` | 04.02 | `ProductMapper`, `ProductController`; the Redis cache from Phase 09 | Before changing what clients receive |
| 05.05 | `backend/src/main/java/com/example/catalog/product/ProductMapper.java` | Explicit DTO ⇄ entity mapping (trims text, prices to two decimals) | 04.03, 05.02–05.04 | `ProductService` | When a field is added |
| 05.06 | `backend/src/main/java/com/example/catalog/product/ProductNotFoundException.java` | Domain error: no product with that id (404) | — | `ProductService`, `ApiExceptionHandler` | With the error handler |
| 05.07 | `backend/src/main/java/com/example/catalog/product/DuplicateSkuException.java` | Domain error: SKU already used (409), translated from MySQL's unique key | — | `ProductService`, `ApiExceptionHandler` | With the error handler |
| 05.08 | `backend/src/main/java/com/example/catalog/product/VersionConflictException.java` | Domain error: outdated version (409), from the version check or Hibernate's `@Version` | — | `ProductService`, `ApiExceptionHandler` | With the error handler |
| 05.09 | `backend/src/main/java/com/example/catalog/error/WritesDisabledException.java` | Domain error: read-only deployment (403) | — | `ProductService`, `ApiExceptionHandler` | With the error handler |
| 05.10 | `backend/src/main/java/com/example/catalog/product/ProductService.java` | Business rules and the transaction boundary for get, create, update, delete | 04.04, 05.01, 05.05–05.09 | `ProductController` | To understand transactions |
| 05.11 | `backend/src/main/java/com/example/catalog/product/ProductController.java` | HTTP endpoints for single products (GET, POST, PUT, DELETE) | 05.10, 05.02–05.04 | Spring MVC | First file to read for the API |
| 05.12 | `backend/src/main/java/com/example/catalog/error/ApiExceptionHandler.java` | Every error as RFC 9457 Problem Details, with field errors | 05.06–05.09, Spring MVC, Jackson 3 | Spring MVC, for every failed request | To understand the error format |
| 05.13 | `backend/src/test/java/com/example/catalog/product/ProductControllerTest.java` | Web-slice tests with a Mockito test double: statuses, JSON, validation, error types | 05.11, 05.12 | Maven Surefire | To see how the HTTP layer is tested in isolation |
| 05.14 | `backend/src/test/java/com/example/catalog/product/ProductServiceTest.java` | Real-transaction tests on MySQL: commit, rollback, version and SKU conflicts, read-only `prod` | 05.10, 04.07 | Maven Surefire | To see transactions proven |

Files the generator produced that we did **not** keep: `HELP.md` (links only), `backend/.gitignore` and
`backend/.gitattributes` (the root files cover them; the useful Java-IDE patterns moved into the root `.gitignore`),
`application.properties` (replaced by the YAML files), and `CatalogApplicationTests` (`HealthEndpointTest` also
starts the application).

<!-- BUILD FLOW 01.06.05 — infra/ and scripts/ (added in Phase 03) -->
## infra/ and scripts/

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 03.01 | `infra/compose.dev.yaml` | Local MySQL 8.4.11 and Redis 8.10.2 containers: pinned images, health checks, `127.0.0.1`-only ports 24306/24379, named MySQL volume, Redis as a pure cache | Docker Desktop; `infra/.env` | `scripts/dev-services.ps1`; the backend's dev profile from Phase 04 | Before changing local services or ports |
| 03.02 | `infra/.env.example` | Template for the private `infra/.env`: database name, user, password placeholders, host ports | — | `scripts/dev-services.ps1` (copies it on the first `up`) | When adding a setting that Compose needs |
| 03.03 | `scripts/dev-services.ps1` | One command for `up`, `status`, `logs`, `down`, and `reset -Force`; creates `infra/.env` with random passwords on the first `up` | `infra/compose.dev.yaml`, `infra/.env.example`, Docker | The developer on Windows | Whenever you start or stop local services |

<!-- BUILD FLOW 01.06.03 — Generated files -->
## Generated files

Generated files are never edited to add comments. Their explanation lives in the rows above.
- Maven Wrapper (Phase 02): `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`.
- Coming later: `package-lock.json` (Phase 12).
