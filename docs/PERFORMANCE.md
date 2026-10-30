<!--
BUILD FLOW 11.06 — docs/PERFORMANCE.md
First introduced: Phase 11
Purpose: How fast the API is under load, and how we know: the benchmark method; product detail with the cache off,
         cold, and warm; search without and with the Phase 08 indexes; what the numbers mean, and their limits.
Depends on: benchmarks/results/ (11.04, 11.05), benchmarks/k6/ (11.01, 11.02), scripts/run-benchmarks.ps1 (11.03),
            decisions D-048 and D-049, docs/QUERY_OPTIMIZATION.md (the Phase 08 query measurements).
Read by: Anyone asking "how fast is it, and how do you know?", including interviewers.
Runtime role: None. Documentation only.
-->

# Performance — whole requests under load, measured with k6

> **Run of 2026-10-30, 18:32, commit `2abde5e`.** Every number on this page comes from
> [`benchmarks/results/k6-report.md`](../benchmarks/results/k6-report.md), which the benchmark script computes from
> the raw k6 summaries in [`k6-summaries.json`](../benchmarks/results/k6-summaries.json). Values are the median of
> three trials, with the lowest and highest trial in brackets. Ratios are computed from those medians.

<!-- BUILD FLOW 11.06.01 — Results in short -->
## Results in short

- **The Redis cache made product details about 3.3 times faster.** With the products in Redis, the API answered
  3,310 requests per second, with a median of 2.35 ms. With the cache off, it answered 1,016 per second, with a median
  of 8.31 ms. Even the slowest cached trial served 2.6 times as many requests as the fastest uncached one.
- **A cache miss costs about twice an uncached read** (median 15.99 ms against 8.31 ms). A miss does the same MySQL
  read, plus a Redis lookup and a Redis write.
- **The Phase 08 indexes made the search mix 3.2 times faster end to end** (144.1 against 44.8 requests per second).
  That is far less than the 73–483 times measured for the page queries alone, for two reasons. Every search also runs
  a `COUNT`, which the indexes barely help. And ten users share four CPU cores.
- **No request failed, and no trial had to be repeated.**

<!-- BUILD FLOW 11.06.02 — What was measured, and how -->
## What was measured, and how

| Scenario | Backend state | What k6 sends | What it shows |
|---|---|---|---|
| Search without indexes | Fresh `catalog_bench` at schema V1 (`--spring.flyway.target=1`) | The four searches below in rotation, 30 s, three trials | Search as built in Phase 06 |
| Search with indexes | Migration V2 applied | The same | The Phase 08 indexes, end to end |
| Product detail, cache off | `catalog.cache.enabled=false` | `GET /api/v1/products/{id}` over 1,000 ids (every 100th product), 30 s, three trials | MySQL on every request |
| Product detail, cold | Cache on; exactly the 1,000 measured keys deleted before each pass | Each of the 1,000 ids once, three passes | Every request a miss (checked: 0 hits, 3,000 misses) |
| Product detail, warm | Cache on, right after the cold passes | The 1,000 ids, 30 s, three trials | Almost every request a hit |

- **A fresh backend for each part, then a 20-second warm-up** whose numbers are thrown away. The warm-up compiles
  the hot Java code and fills MySQL's buffer pool. The cache-on warm-up reads other ids (every 100th product + 50), so
  the JVM is warm but Redis holds none of the measured keys.
- **A closed model with 10 virtual users.** Each user sends its next request as soon as the previous one has
  answered, with no pause. Throughput and latency are then two views of one thing: users = throughput × time per
  request (Little's law). For example, 10 users ÷ 9.82 ms per request = 1,018 requests per second, and k6 measured
  1,016 (cache off). So "3.3 times the throughput" and "3.3 times less time per request" are the same result.
- **The data:** 100,000 generated products, seed 20261022 (the Phase 08 dataset).
- **The machine:** one laptop for everything (Intel i5-11320H, 4 cores / 8 threads, 15.8 GB RAM, Windows 11, on mains
  power). k6 2.3.0 runs in Docker and reaches the backend on `127.0.0.1:24083` through Docker Desktop's
  `host.docker.internal`. MySQL 8.4.11 and Redis 8.10.2 come from the development Compose file.
- **Two settings for benchmark runs only**, passed on the command line: the metrics endpoint on `127.0.0.1`, for the
  cache counters; and `server.tomcat.max-keep-alive-requests=-1`, so that each user keeps one connection (D-049).
- **When a run counts** (D-049): every request got a response, every connection opened within 5 s, every timed
  trial ended within 5 s of its 30 s, and Windows logged no sleep or standby. A cold pass must also show exactly 0 hits
  and 1,000 misses. In this run all 15 trials passed on their first attempt. They lasted 30.0–30.1 s, and the slowest
  connection opened in 21.7 ms.

<!-- BUILD FLOW 11.06.03 — Product detail -->
## Product detail: cache off, cold, and warm

| Scenario | Throughput (req/s) | Mean (ms) | Median (ms) | p95 (ms) | Cache hits / misses (all trials) |
|---|---:|---:|---:|---:|---|
| Cache off | 1,016.0 (916.1 to 1,208.0) | 9.55 (8.04 to 10.60) | 8.31 (7.18 to 9.29) | 16.75 (12.77 to 18.55) | 0 / 0 (not used) |
| Cache on, cold | 578.5 (570.1 to 699.9) | 16.86 (13.87 to 17.07) | 15.99 (13.30 to 16.15) | 23.82 (20.44 to 24.02) | 0 / 3,000 |
| Cache on, warm | 3,310.0 (3,188.0 to 3,617.2) | 2.87 (2.62 to 2.98) | 2.35 (2.32 to 2.52) | 5.35 (4.23 to 5.94) | 302,285 / 1,162 |

**What it means**
- **A hit skips the database completely.** There is no transaction, no SQL, and no mapping from the entity: one Redis
  `GET`, then JSON. The median fell 3.5 times (8.31 → 2.35 ms), and the p95 fell 3.1 times (16.75 → 5.35 ms).
- **A miss is the expensive case.** It is the uncached read plus a Redis lookup before it and a Redis write after it.
  Here it took 1.9 times as long as an uncached read. This run doesn't break the extra time down, because no
  per-step timing was recorded. The two extra Redis round trips are part of it. Another part may be the shape of a
  cold pass: a burst of under two seconds.
- **A rough break-even, from these medians only.** If a product is read *n* times while its copy lives (60 s), the
  cache costs 15.99 + (*n* − 1) × 2.35 ms in total, against *n* × 8.31 ms without it. These are equal at *n* ≈ 2.3.
  So a product read once a minute is slower with the cache, and one read three or more times a minute is faster. Real
  catalogs have a few very popular products, which is why caching product pages works well.
- **The warm trials still missed 1,162 times, all in trial 2.** That was the TTL at work, not a fault:
  - The last cold pass had stored all 1,000 keys just before trial 1, and their 60 seconds ran out during trial 2.
  - Each key was fetched again once, which accounts for 1,000 of the misses.
  - The other 162 were a second request for the same product arriving before the first had stored the fresh copy.
    That is the small-scale stampede described in [`CACHING.md`](CACHING.md).

  Redis evicted no keys (`evicted_keys:0` after the run), and the hit ratio over the three trials was 99.62%.

<!-- BUILD FLOW 11.06.04 — Search -->
## Search: without and with the Phase 08 indexes

| Search | Without indexes: median (ms) | With indexes: median (ms) | Faster | With indexes: p95 (ms) |
|---|---:|---:|---:|---:|
| home-newest (`GET /api/v1/products`) | 188.73 (145.96 to 221.08) | 45.93 (44.88 to 56.98) | 4.1× | 90.92 (86.45 to 104.94) |
| category-newest (`?category=HOME`) | 190.63 (156.82 to 225.46) | 32.54 (32.02 to 41.39) | 5.9× | 73.73 (66.01 to 90.30) |
| price-range-in-stock (`?minPrice=1000&maxPrice=2000&inStock=true&sort=price_asc`) | 225.00 (186.85 to 263.22) | 82.67 (80.38 to 95.10) | 2.7× | 139.32 (136.13 to 161.29) |
| name-contains-common (`?q=linen`) | 252.64 (211.42 to 292.98) | 90.92 (88.96 to 104.13) | 2.8× | 148.95 (139.80 to 167.51) |
| **All four, throughput** | 44.8 req/s (38.1 to 54.5) | 144.1 req/s (123.4 to 148.5) | 3.2× | |

**Why only 3–6 times, when Phase 08 made the page queries 73–483 times faster**
- **Every search runs two queries:** the page and a `COUNT` for the total. In Phase 08's single-query measurements
  ([`QUERY_OPTIMIZATION.md`](QUERY_OPTIMIZATION.md)), the indexes cut the four page queries to 0.1–0.7 ms, but the
  counts still take 3.9–33.3 ms. Only the category count got faster. The count is now most of the database work of a
  search.
- **The order of the results follows the cost of the counts.** With the indexes, the category search is fastest:
  its count takes 3.89 ms, and the whole request 32.54 ms. The name search is slowest: its count takes 33.3 ms, and
  the whole request 90.92 ms.
- **Most of each request's time is waiting, not working.** By Phase 08's numbers, one rotation of the four searches
  needs about 256 ms of MySQL work without the indexes and about 68 ms with them: 3.7 times less. Throughput rose
  3.2 times.
- **A rough estimate of the load.** At the measured rates, MySQL alone needs about 2.5–2.9 seconds of work every
  second, that is, two to three of the four cores. The JVM, k6, and Docker need the rest. So a request that costs
  4 ms of MySQL work (the category search) still takes 32.54 ms, because it queues for a CPU. CPU use wasn't
  recorded, so treat this as an estimate.
- **What would help next** is the counts, not more indexes. The options are in decision D-041 and in
  `QUERY_OPTIMIZATION.md`: an approximate total ("10,000+"), a cached count, or no total at all.

<!-- BUILD FLOW 11.06.05 — Limits -->
## How far these numbers go

- **One laptop runs everything.** k6, the backend, MySQL, Redis, and Docker's network forwarding share four cores.
  The absolute numbers belong to this machine and this moment. The comparisons between scenarios are the result.
- **The trials varied, and they drifted.** In the cache-off and search runs, throughput rose from trial to trial
  (cache off: 916 → 1,016 → 1,208 requests per second). In the warm-cache trials it fell (3,617 → 3,310 → 3,188). The
  laptop's other work and its clock speed change over minutes, and nothing here controls them. That is why every
  table shows the lowest and highest trial. The conclusions hold even at the edges of these ranges: the weakest
  warm-cache trial served 2.6 times the strongest cache-off trial, and the weakest indexed search trial served
  2.3 times the strongest unindexed one.
- **The closed model measures one level of concurrency.** It measures throughput at exactly 10 busy users. It does
  not measure latency at a given arrival rate. With fewer users, or an open model below capacity, there is less
  queueing and latencies would be lower.
- **Connections stay open** (`max-keep-alive-requests=-1`). This removes Tomcat's reconnect every 100 requests from
  every scenario alike, so absolute numbers are slightly better than with Tomcat's default.
- **The access pattern is simple.** All 1,000 measured products fit in Redis, and they are read evenly. With a
  catalog larger than the cache, the hit ratio would depend on how popular each product is.
- **Not measured here:**
  - a stopped or slow Redis (Phase 10 measured it by hand: about 430 ms per read, with correct data);
  - writes;
  - the rare-word search and the deepest page (measured inside MySQL in Phase 08 only);
  - more data or more users;
  - a real deployment (Phase 15).

<!-- BUILD FLOW 11.06.06 — Runs that were discarded -->
## Runs that were discarded, and what each one changed

Four earlier runs are not reported anywhere. Each one exposed a way a benchmark can produce wrong numbers without
looking wrong, and each led to a check in `scripts/run-benchmarks.ps1` (D-049).

| Run | What went wrong | The check it led to |
|---|---|---|
| 2026-10-29, 14:04 | The laptop entered Modern Standby whenever its display went off. Two 30-second trials lasted 54 minutes and about two hours. | The script keeps the computer awake, refuses to start on battery, and discards a run if Windows logged sleep or standby. A trial that runs long is repeated. |
| 2026-10-29, 17:19 | The first trial lasted 48 s. Two requests could not open a connection for 30 s, while their measured durations stayed at 0 ms. | k6's full output is kept. A trial with a request that couldn't connect, or a slow connection, is set aside and repeated. Any other failed request stops the run. |
| 2026-10-30, 12:34 | Three product-detail attempts in a row had connections that took 19.5 s to open or timed out. The cause was a new connection every 100 requests, through Docker Desktop's forwarding. | Connections stay open for benchmark runs (`max-keep-alive-requests=-1`). |
| 2026-10-30, 12:46 | Clean by every rule so far, but the "cold" passes weren't cold: 2,000 of 3,000 requests were hits. Deleting the keys had silently failed, because Windows PowerShell 5.1 put a byte-order mark in front of the `DEL` command it piped to `redis-cli`. | The keys are passed as arguments, and Redis's answer is checked. Every cold pass must show exactly 0 hits and 1,000 misses. |

<!-- BUILD FLOW 11.06.07 — Reproducing the results -->
## Reproducing the results

```powershell
.\scripts\dev-services.ps1 up            # MySQL and Redis (Phase 03)
cd backend; .\mvnw.cmd clean verify; cd ..   # builds the jar the script runs
.\scripts\run-benchmarks.ps1             # about ten minutes; mains power, lid open
```

The script rebuilds the local `catalog_bench` database (never `catalog`), starts and stops the backend itself, and
overwrites the two files in `benchmarks/results/`. It writes nothing there unless the whole run passes. Use
`-SkipIndexComparison` to keep `catalog_bench` and skip the "without indexes" part. Use `-AllowBattery` to measure on
battery anyway (the setup table then says so). k6's output and the backend logs of each run stay in a scratch folder
that the script prints at the end.
