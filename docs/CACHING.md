<!--
BUILD FLOW 09.05 — docs/CACHING.md
First introduced: Phase 09
Purpose: How product details are cached in Redis: what is cached and what is not, the key and value format, the TTL,
         the read flow, the after-commit eviction on writes (since Phase 10), failure behavior, metrics, and the
         consistency limits that remain.
Depends on: ProductDetailCache (09.02), ProductService (05.10), ProductCacheEvictionListener (10.02),
            ProductCacheProperties (09.01), application.yml (02.06.06–02.06.08), application-dev.yml (02.07.06–02.07.07).
Read by: Anyone changing the cache, and anyone asking "can this serve stale data?" (the answer is yes, and when).
Runtime role: None. Documentation only.
-->

# Caching — product details in Redis

> **Status (Phase 10):** on by default. Reads use cache-aside; every committed create, update, and delete removes the
> product's key; Redis failures fall back to MySQL within a fraction of a second. Phase 11 measures the effect.

<!-- BUILD FLOW 09.05.01 — What is cached -->
## What is cached, and what is not

- **Cached:** the response of `GET /api/v1/products/{id}`, one product's `ProductResponse` DTO. A product page is read
  far more often than a product changes, and one key maps to exactly one product, so every write knows which single
  key to clear.
- **Not cached: search.** A search result depends on many parameters, and one product change can alter thousands of
  cached result pages (every filter, sort, and page it appears in). Clearing them correctly is a hard problem; search
  stays on MySQL with the Phase 08 indexes (decision D-008).
- **Not cached: missing products.** A 404 is not stored ("negative caching"). Otherwise a product created a moment
  later would stay invisible until the TTL expired.

<!-- BUILD FLOW 09.05.02 — Keys and values -->
## Keys and values

| Item | Choice | Why |
|---|---|---|
| Key | `catalog:v1:product:{id}`, for example `catalog:v1:product:7` | `catalog` separates this application's keys from anything else in the same Redis; `v1` names the value format, so an incompatible future format uses `v2` keys instead of misreading old values |
| Value | The `ProductResponse` as JSON, written by the application's own Jackson 3 mapper | It looks exactly like the API's answer (`"price":6990.00`, UTC timestamps), and it is readable with `redis-cli` |
| TTL | 60 seconds (`catalog.cache.product-ttl`) | Redis deletes the key by itself. A longer TTL gives more hits; a shorter one limits how long an outdated copy can live when an eviction is missed |
| Switch | `catalog.cache.enabled` (default `true` since Phase 10) | Benchmarks compare the same endpoint with the cache on and off (Phase 11) |

<!-- BUILD FLOW 09.05.03 — The read flow -->
## Reading (cache-aside)

"Cache-aside" means the application manages the cache next to the database: it looks in the cache first, and fills
it itself after a database read. `ProductService.getProduct` does this in three steps (runtime steps in
`REQUEST_FLOWS.md`, Flow 2):

1. **Look up** `catalog:v1:product:{id}` (`READ-04`). Found: return it; MySQL is not touched (Flow 3, a hit).
2. **Not found:** read MySQL in a read-only transaction and build the DTO (`READ-05`, `READ-06`). A missing product
   throws here, so nothing is stored.
3. **Store** the DTO with the TTL (`READ-07`), after the transaction has ended.

The transaction covers only step 2. With `@Transactional` on the whole method, every request, even a cache hit,
would borrow a database connection, and the Redis calls would run while holding it.

<!-- BUILD FLOW 09.05.08 — Writes -->
## Writing: evict after the commit (since Phase 10)

1. `createProduct`, `updateProduct`, and `deleteProduct` publish `ProductChangedEvent(id)` once their SQL has
   succeeded, still inside their transaction. For a create, the id is the one MySQL just generated.
2. Spring holds the event. If the transaction **commits**, it calls `ProductCacheEvictionListener`
   (`@TransactionalEventListener(phase = AFTER_COMMIT)`), which deletes `catalog:v1:product:{id}` (`WRITE-07`). If the
   transaction **rolls back**, the listener never runs, and nothing needs clearing, because MySQL did not change.
3. The listener runs in the request's own thread before the response is sent, so the client that made the change
   reads the new version next.

**Why not evict inside the transaction?** Between the eviction and the commit, another request could miss, read the
old row (the change is not committed yet), and store it again. After the commit, MySQL already holds the new row.
**Why not update the cached value instead of deleting it?** Two concurrent writers could store their values in the
opposite order from their commits. A delete is idempotent: the next read stores whatever is committed.

<!-- BUILD FLOW 09.05.04 — Failures -->
## When Redis fails

- **Short timeouts.** Redis commands give up after 200 ms (`spring.data.redis.timeout`, D-047); the client's default
  is 60 seconds. A healthy local Redis answers in well under a millisecond.
- **Reads fall back to MySQL.** A failed lookup or store is logged, counted as `error`, and the request continues with
  MySQL. Only Spring's `DataAccessException` and Jackson's `JacksonException` are caught; anything else is a bug and
  surfaces.
- **A write never fails because of its eviction.** A failed eviction is logged and counted
  (`catalog.product.cache.evictions`, `outcome=failed`); the write has committed and is reported as the success it is.
  Even an unexpected exception could not change that: Spring runs after-commit listeners in its `afterCompletion`
  callback, which logs exceptions instead of rethrowing them.
- **Health ignores Redis** (`management.health.redis.enabled: false`, D-042): a cache outage must not mark the whole
  service DOWN.
- **What it costs.** Measured on 2026-10-29 with Redis **stopped** in the dev stack: every product read still answered
  correctly in about 430 ms (two 200 ms timeouts plus MySQL) instead of about 10–15 ms; an update succeeded in 253 ms
  (its eviction timed out). With Redis **paused** in `ProductCacheOutageTest` (a hung server), each cache step gave up
  after about 200–230 ms. An extension could make a stopped Redis cost almost nothing (reject commands at once while
  disconnected, or a circuit breaker that stops trying for a while); a hung Redis is always bounded by the timeout.
- **A timeout does not cancel the command.** Commands already sent to a hung Redis still run when it resumes. A
  "failed" eviction may therefore still happen later, and a late store may still write a value.

<!-- BUILD FLOW 09.05.05 — Metrics -->
## Metrics

| Counter | Tag | Meaning |
|---|---|---|
| `catalog.product.cache` | `result=hit`, `miss`, `error` | Lookups and failed lookups or stores; hit rate = hits / (hits + misses) |
| `catalog.product.cache.evictions` | `outcome=done`, `failed` | Evictions after committed writes; a failure can leave an outdated copy until its TTL ends |

With the dev profile:

```powershell
curl.exe -s "http://localhost:24081/actuator/metrics/catalog.product.cache?tag=result:hit"
curl.exe -s "http://localhost:24081/actuator/metrics/catalog.product.cache.evictions?tag=outcome:failed"
```

Production exposes only the health endpoint, so these counters stay internal there.

<!-- BUILD FLOW 09.05.06 — Checked by hand -->
## Looking at the cache by hand

With `.\scripts\dev-services.ps1 up` and the backend started with the dev profile:

```powershell
curl.exe -s http://localhost:24081/api/v1/products/7 > $null
docker compose --env-file infra\.env -f infra\compose.dev.yaml exec -T redis redis-cli --scan --pattern "catalog:v1:product:*"
docker compose --env-file infra\.env -f infra\compose.dev.yaml exec -T redis redis-cli TTL catalog:v1:product:7
docker compose --env-file infra\.env -f infra\compose.dev.yaml exec -T redis redis-cli GET catalog:v1:product:7
```

Seen on 2026-10-23 (Phase 09): one key after the first read, `TTL` 59 seconds, the value exactly the API's JSON.
Seen on 2026-10-29 (Phase 10): after a `PUT`, the key was gone at once and the next read showed the new version; after
a `DELETE`, the key was gone and the read answered 404. The first request of a freshly started JVM is always slow
(classes load, code compiles), so single timings from a fresh start are not cache measurements; Phase 11 measures.

<!-- BUILD FLOW 09.05.07 — Limits -->
## Consistency limits that remain

**The stale-read race.** Cache-aside with eviction can still serve an outdated product. One concrete order of events:

1. Request A misses the cache and reads product 42, version 7, from MySQL. It is slow to continue.
2. Request B updates product 42 to version 8, commits, and evicts `catalog:v1:product:42`, which is not there yet.
3. Request A stores its version-7 copy.
4. Every reader gets version 7 until the TTL ends, at most 60 seconds later.

`ProductCacheInvalidationTest.aSlowReaderCanStillStoreAnOldCopyAfterAWrite` plays exactly these steps and shows the
outdated copy being served. The window needs a reader to be slower than a whole write, so it is rare, and the TTL bounds
it. Stronger approaches are extensions, not built: storing only if the cached version is not older (a compare-and-set
in a Redis script), a short delay and a second eviction after each write, or invalidation driven by the database's
change log.

**A failed eviction** (Redis down during a write) leaves the old copy until its TTL ends, unless Redis lost its data
anyway. The dev Redis keeps nothing on disk (D-019), so a restarted one starts empty; a Redis that only hung keeps its
keys.

**Switching the cache off and on.** While the switch is off, writes do not evict. After switching it back on, copies
cached before the switch can be up to one TTL old; waiting 60 seconds removes them.

**Cache stampede.** When a popular key expires, many simultaneous requests can miss together and all read MySQL at
once. At this scale that is harmless (a product read takes milliseconds); request coalescing (one request reloads,
the others wait), early refresh before expiry, or a little random jitter on the TTL are documented extensions.

**No `FLUSHALL`, no broad deletes, no public cache endpoint.** Keys are removed one by one, by id, and only by writes.
