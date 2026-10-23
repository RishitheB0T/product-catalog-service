<!--
BUILD FLOW 09.05 — docs/CACHING.md
First introduced: Phase 09
Purpose: How product details are cached in Redis: what is cached and what is not, the key and value format, the TTL,
         the read flow, failure behavior, metrics, and the limits that Phase 10 still has to close.
Depends on: ProductDetailCache (09.02), ProductService.getProduct (05.10), ProductCacheProperties (09.01),
            application.yml (02.06.06–02.06.07), application-dev.yml (02.07.06–02.07.07).
Read by: Anyone changing the cache, and anyone asking "can this serve stale data?" (the answer is yes, and when).
Runtime role: None. Documentation only.
-->

# Caching — product details in Redis

> **Status (Phase 09):** the read path is built and tested, and the cache is **switched off by default**. Creating,
> updating, or deleting a product does not clear its cached copy yet: Phase 10 adds after-commit eviction, short
> Redis timeouts, outage tests, and then switches the cache on. Until then an enabled cache can serve a product that
> changed in the last 60 seconds.

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
| TTL | 60 seconds (`catalog.cache.product-ttl`) | Redis deletes the key by itself. A longer TTL gives more hits; a shorter one limits how long an outdated copy can live |
| Switch | `catalog.cache.enabled` (default `false` in Phase 09) | Benchmarks compare the same endpoint with the cache on and off (Phase 11) |

<!-- BUILD FLOW 09.05.03 — The read flow -->
## The read flow (cache-aside)

"Cache-aside" means the application manages the cache next to the database: it looks in the cache first, and fills
it itself after a database read. `ProductService.getProduct` does this in three steps (runtime steps in
`REQUEST_FLOWS.md`, Flow 2):

1. **Look up** `catalog:v1:product:{id}` (`READ-04`). Found: return it; MySQL is not touched (Flow 3, a hit).
2. **Not found:** read MySQL in a read-only transaction and build the DTO (`READ-05`, `READ-06`). A missing product
   throws here, so nothing is stored.
3. **Store** the DTO with the TTL (`READ-07`), after the transaction has ended.

The transaction covers only step 2. With `@Transactional` on the whole method, every request, even a cache hit,
would borrow a database connection, and the Redis calls would run while holding it.

<!-- BUILD FLOW 09.05.04 — Failures -->
## When something goes wrong

- **A Redis error or an unreadable value** (for example "not json" under a key) is logged, counted as an `error`,
  and treated like a miss: the request continues with MySQL, and the fresh value overwrites the bad one. Only
  Spring's `DataAccessException` (Redis connection and command errors) and Jackson's `JacksonException` are caught;
  any other exception is a bug and still surfaces.
- **A failed write to Redis** is logged and counted, and the request still succeeds with the product from MySQL.
- **Redis down: not handled well yet.** The Redis client (Lettuce) waits up to 60 seconds for a command by default,
  so a request could hang that long. Phase 10 adds short timeouts and tests the outage.
- **Health ignores Redis** (`management.health.redis.enabled: false`, decision D-042). The cache only speeds up
  reads; a cache outage must not mark the whole service DOWN.

<!-- BUILD FLOW 09.05.05 — Metrics -->
## Metrics

One counter, `catalog.product.cache`, with the tag `result` = `hit`, `miss`, or `error`. The hit rate is
hits / (hits + misses). With the dev profile:

```powershell
curl.exe -s "http://localhost:24081/actuator/metrics/catalog.product.cache?tag=result:hit"
```

Production exposes only the health endpoint, so these counters stay internal there.

<!-- BUILD FLOW 09.05.06 — Checked by hand -->
## Looking at the cache by hand

With `.\scripts\dev-services.ps1 up` and the backend started with the dev profile plus `--catalog.cache.enabled=true`:

```powershell
curl.exe -s http://localhost:24081/api/v1/products/7 > $null
docker compose --env-file infra\.env -f infra\compose.dev.yaml exec -T redis redis-cli --scan --pattern "catalog:v1:product:*"
docker compose --env-file infra\.env -f infra\compose.dev.yaml exec -T redis redis-cli TTL catalog:v1:product:7
docker compose --env-file infra\.env -f infra\compose.dev.yaml exec -T redis redis-cli GET catalog:v1:product:7
```

Seen on 2026-10-23: one key after the first read, `TTL` 59 seconds, the value exactly the API's JSON; metrics
hit = 1, miss = 1, error = 0 after two reads; `GET /api/v1/products/999999` answered 404 and left no key. The first
request took 0.71 s and the second 0.02 s, but the first request of a freshly started JVM is always slow (classes
load, code compiles), so this is not a cache measurement. Phase 11 compares cache on and off properly.

<!-- BUILD FLOW 09.05.07 — Limits -->
## Limits, and what Phase 10 adds

- **Writes do not clear the cache yet.** Phase 10 deletes `catalog:v1:product:{id}` after a create, update, or
  delete transaction has **committed** (an after-commit listener), never before: clearing it before the commit would
  let another request put the old version back while the transaction is still open.
- **Even then, a stale read stays possible** (the race in `REQUEST_FLOWS.md`, "Known limit"): a slow reader can store
  an old copy just after a writer cleared the key. The TTL bounds how long it lives.
- **Cache stampede:** when a popular key expires, many simultaneous requests can miss together and all read MySQL.
  At this scale that is harmless; locking or early refresh are documented extensions, not built.
- **No `FLUSHALL`, no broad deletes, no public cache endpoint.** Keys are removed one by one, by id.
