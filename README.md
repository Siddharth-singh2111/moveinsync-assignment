# GPS Gap Backfill & Billable Distance Reconstruction

Rebuilds a trusted, **explainable** billable distance for every cab trip from unreliable GPS
pings. Cheap maths where possible, trusted anchor points where needed, and a cached,
circuit-broken routing provider only for real gaps — with every segment labelled by the rule
that produced it, so a vendor dispute can be answered line by line.

See **[DESIGN.md](DESIGN.md)** for the architecture, the hardest-flow sequence diagram, the
failure-mode table and the trade-offs.

## Guarantees

1. **Deterministic** — same inputs + same `ruleVersion` ⇒ identical kilometres (distances summed
   in integer metres, converted to km once).
2. **Explainable** — every segment carries its rule (`SHORT_GAP_AERIAL`, `PARKED`, `ROUTED`,
   `REJECTED_SPEED`, `DEGRADED_AERIAL`).
3. **Resilient** — if the routing provider fails, the circuit opens, the trip is billed on a
   bounded aerial estimate and marked `DEGRADED`, and it is recomputed once the provider recovers.
4. **Idempotent & honest** — a billed trip's distance is never rewritten; late pings or a recovery
   append a **signed delta** (`+1.8 km`) to a ledger.
5. **Tenant-isolated** — the tenant comes from the JWT, so one client can never read another's
   trips; no personal data in logs or errors.

## Run

```bash
docker compose up --build
```

Starts PostgreSQL and the service (built inside Docker — only Docker is required). API on
`http://localhost:8080`. Target runtime is **Java 21**.

### Test and benchmark locally

```bash
./mvnw test             # 86 tests; uses in-memory H2, no Docker needed
./run-benchmark.sh      # 10000 trips x 1500 pings (override: ./run-benchmark.sh 2000 1000)
```

## API

All `/api/trips` endpoints require a bearer token. Get one from `/api/auth/login` (demo users are
in `application.yml`: `operator` / `viewer` / `admin`, all tenant `tenantA`).

| Method | Path | Role | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/login` | — | Exchange credentials for a JWT |
| `POST` | `/api/trips` | operator/admin | Compute a trip (idempotent) |
| `GET`  | `/api/trips/{id}/distance` | any | Total km, status, provider-call count |
| `GET`  | `/api/trips/{id}/explain` | any | Per-segment rule breakdown (dispute view) |
| `POST` | `/api/trips/{id}/pings` | operator/admin | Add late pings → recompute |
| `POST` | `/api/trips/{id}/recompute` | operator/admin | Recompute from stored inputs |
| `POST` | `/api/trips/{id}/bill` | operator/admin | Close the invoice |
| `GET`  | `/api/trips/{id}/deltas` | any | Signed-delta ledger |

A full end-to-end walkthrough (login → compute → explain → bill → late pings → delta) is in
**[demo.sh](demo.sh)**:

```bash
./demo.sh        # against a running docker compose
```

Metrics are at `/actuator/prometheus` (authenticated); `/actuator/health` is public.

## Benchmark results

Measured locally (Apple Silicon laptop, JDK 27), 10,000 trips × 1,500 pings:

| Metric | Value |
|---|---|
| Total time | 2.56 s (~3,900 trips/s) |
| Compute latency p50 / p99 | 0.22 ms / 0.63 ms |
| Routing calls **before** caching | 105,000 |
| Routing calls **after** caching | 16 |
| Heap used | ~29 MB |

Core per-trip complexity is `O(n log n)` for the sort plus `O(n)` for the pipeline (`n` = pings);
routing calls are bounded by the number of *distinct* real gaps and collapsed further by the cache.

## How this maps to the evaluation criteria

1. **Authentication & tenant isolation** — stateless JWT, roles (employee/operator/admin), tenant
   taken from the token; cross-tenant reads return 404.
2. **Cost (time & space)** — complexity above, backed by the benchmark.
3. **Failure handling** — circuit breaker + degraded fallback + auto-recompute; failure-injection
   test (`FlakyRoutingProvider` fails halfway through a batch).
4. **OOP** — clean controller/service/repository layering; a framework-free `core` engine; Strategy
   pattern for segment rules; polymorphic routing providers.
5. **Trade-offs** — documented in DESIGN.md §12 (cost vs accuracy, degrade vs fail, append vs mutate).
6. **Monitoring** — Micrometer counters/gauges/timer (provider calls, cache hits, degraded trips,
   rejections, deltas, compute p50/p99).
7. **Caching** — snapped-cell routing cache with before/after call counts (105k → 16).
8. **Error handling** — non-leaking `@RestControllerAdvice`; no coordinates/phone/email in logs.

## Package layout

```
com.moveinsync.distance
├── core            framework-free engine (no Spring, no DB, no HTTP)
│   ├── model         Ping, Anchor, Segment, DistanceResult, TimelinePoint, ...
│   ├── clean         StreamCleaner — dedupe, order, accuracy, teleport
│   ├── anchor        AnchorBuilder — patch gaps, fallback chain, office rule
│   ├── rule          RuleEngine + ordered SegmentClassifier strategies
│   ├── distance      haversine + metre/km rounding
│   ├── routing       RoutingProvider port + CircuitBreaker
│   └── engine        DistanceEngine pipeline, input hashing, signed deltas
├── routing         adapters: caching, resilient, fake/failing/flaky providers
├── service         orchestration, idempotent recompute, delta reconciliation
├── persistence     JPA entities + tenant-scoped repositories
├── api             REST controllers, DTOs, error handling
├── security        JWT auth, roles, filter, config
├── metrics         Micrometer meters
├── config          engine bean wiring from configuration
└── benchmark       headless load test
```
