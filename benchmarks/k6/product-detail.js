// BUILD FLOW 11.01 — benchmarks/k6/product-detail.js
// First introduced: Phase 11
// Purpose: Load for GET /api/v1/products/{id} over a fixed set of 1,000 product ids, either for a fixed time with a
//          fixed number of virtual users ("load"), or one request per id ("pass", used for the cold-cache run).
// Depends on: The backend with the bench profile (100,000 products), reachable from the k6 container.
// Used by: scripts/run-benchmarks.ps1 (11.03), which runs it in the grafana/k6:2.3.0 container.
// Runtime role: Benchmark tooling only; never part of the application.

import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

// BUILD FLOW 11.01.01 — Settings, passed in as environment variables by the orchestrator
// k6 runs this file in its own JavaScript engine (not Node.js); __ENV holds the -e values from the command line.
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:24083';
const MODE = __ENV.MODE || 'load';
const VUS = parseInt(__ENV.VUS || '10', 10);
const DURATION = __ENV.DURATION || '30s';
const ID_OFFSET = parseInt(__ENV.ID_OFFSET || '100', 10);
const LABEL = __ENV.LABEL || 'product-detail';

// BUILD FLOW 11.01.02 — The product ids
// Every 100th product: 100, 200, ..., 100,000 (1,000 ids spread over the whole table). The warm-up uses ID_OFFSET=50
// (50, 150, ..., 99,950), which shares no id with the measured set. No randomness: every run requests the same ids.
const COUNT = 1000;
const STEP = 100;
const ids = Array.from({ length: COUNT }, (_, i) => ID_OFFSET + i * STEP);

// BUILD FLOW 11.01.03 — Two ways of running
// load: "constant-vus", a closed model. Each virtual user sends its next request as soon as the previous one has
//       answered, so throughput and latency are measured together at a fixed concurrency.
// pass: "shared-iterations", exactly one request per id, shared by the virtual users: with a fresh cache, every request
//       is a miss.
export const options = {
  scenarios: MODE === 'pass'
    ? { pass: { executor: 'shared-iterations', vus: VUS, iterations: COUNT, maxDuration: '5m' } }
    : { load: { executor: 'constant-vus', vus: VUS, duration: DURATION } },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  // Only the status matters here; not keeping bodies saves the load generator's memory and CPU.
  discardResponseBodies: true,
};

// BUILD FLOW 11.01.04 — One request
// In "load" mode each virtual user walks the ids from its own starting point (7919 is a prime, so the starting points
// spread out); in "pass" mode the shared iteration number picks the id.
export default function () {
  const index = MODE === 'pass'
    ? exec.scenario.iterationInTest
    : (exec.vu.idInTest * 7919 + exec.vu.iterationInScenario) % COUNT;
  const response = http.get(`${BASE_URL}/api/v1/products/${ids[index]}`, { tags: { name: 'product-detail' } });
  check(response, { 'status is 200': (r) => r.status === 200 });
}

// BUILD FLOW 11.01.05 — The result file
// handleSummary replaces k6's printed summary: the complete summary goes to /results/<label>.json (a folder the
// orchestrator mounts), and one line goes to the console.
export function handleSummary(data) {
  const requests = data.metrics.http_reqs.values;
  const duration = data.metrics.http_req_duration.values;
  const line = `${LABEL}: ${requests.count} requests, ${requests.rate.toFixed(1)}/s, median ${duration.med.toFixed(2)} ms,`
    + ` p95 ${duration['p(95)'].toFixed(2)} ms, failed ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
  return { [`/results/${LABEL}.json`]: JSON.stringify(data, null, 2), stdout: line };
}
