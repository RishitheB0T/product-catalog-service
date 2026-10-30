// BUILD FLOW 11.02 — benchmarks/k6/search.js
// First introduced: Phase 11
// Purpose: Load for search: the four storefront searches measured in Phase 08, sent in strict rotation by a fixed
//          number of virtual users for a fixed time, each tagged with its name so it is reported separately.
// Depends on: The backend with the bench profile (100,000 products), reachable from the k6 container.
// Used by: scripts/run-benchmarks.ps1 (11.03), with and without the Phase 08 indexes.
// Runtime role: Benchmark tooling only; never part of the application.

import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

// BUILD FLOW 11.02.01 — Settings, passed in as environment variables by the orchestrator
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:24083';
const VUS = parseInt(__ENV.VUS || '10', 10);
const DURATION = __ENV.DURATION || '30s';
const LABEL = __ENV.LABEL || 'search';

// BUILD FLOW 11.02.02 — The request mix: the same searches as benchmarks/queries/search-queries.sql
// Equal shares, in rotation. The rare-word search and the deepest page are left out on purpose: they are exceptions
// measured in Phase 08, and a few slow requests would dominate a closed-model run.
const SEARCHES = [
  { name: 'home-newest', path: '/api/v1/products' },
  { name: 'category-newest', path: '/api/v1/products?category=HOME' },
  { name: 'price-range-in-stock', path: '/api/v1/products?minPrice=1000&maxPrice=2000&inStock=true&sort=price_asc' },
  { name: 'name-contains-common', path: '/api/v1/products?q=linen' },
];

// BUILD FLOW 11.02.03 — Options
// One threshold per search that every run passes (max >= 0): k6 only reports a tagged sub-metric, such as the duration
// of "home-newest" alone, when a threshold names it.
const thresholds = {};
for (const search of SEARCHES) {
  thresholds[`http_req_duration{name:${search.name}}`] = ['max>=0'];
  thresholds[`http_reqs{name:${search.name}}`] = ['count>=0'];
}

export const options = {
  scenarios: { load: { executor: 'constant-vus', vus: VUS, duration: DURATION } },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  discardResponseBodies: true,
  thresholds,
};

// BUILD FLOW 11.02.04 — One request
export default function () {
  const search = SEARCHES[(exec.vu.idInTest + exec.vu.iterationInScenario) % SEARCHES.length];
  const response = http.get(`${BASE_URL}${search.path}`, { tags: { name: search.name } });
  check(response, { 'status is 200': (r) => r.status === 200 });
}

// BUILD FLOW 11.02.05 — The result file (as in product-detail.js)
export function handleSummary(data) {
  const requests = data.metrics.http_reqs.values;
  const line = `${LABEL}: ${requests.count} requests, ${requests.rate.toFixed(1)}/s, failed `
    + `${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
  return { [`/results/${LABEL}.json`]: JSON.stringify(data, null, 2), stdout: line };
}
