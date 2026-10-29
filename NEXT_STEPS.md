# Implementation Roadmap & Handoff

Status and remaining work for the GPS distance reconstruction service. See
**[DESIGN.md](DESIGN.md)** for the architecture, sequence diagram, failure modes and trade-offs.

## Current status

The framework-free engine and the first half of the Spring platform are complete and tested.
**76 tests pass.**

Done:
- **Core engine** — domain model; haversine + single metre/km rounding; `StreamCleaner`
  (dedupe, order, accuracy, teleport); `AnchorBuilder` (fallback chain, 400 m office rule,
  no-show flag); `RoutingProvider` port with deterministic fake/failing implementations;
  `RuleEngine` (ordered classifier chain → labelled segments); `DistanceEngine` (full pipeline,
  `inputHash`, determinism golden test).
- **Trust features** — `CachingRoutingProvider` (snapped-cell key + calls-before/after metrics);
  `CircuitBreaker` + `ResilientRoutingProvider` + `DEGRADED` fallback; `Deltas` (idempotent
  recompute + signed-delta reconciliation, never mutate a billed total).
- **Platform (partial)** — JPA entities + tenant-scoped repositories; `EngineConfig` building the
  engine from `DistanceProperties`; `TripService` (billed-vs-unbilled store logic, appends deltas);
  REST API (`TripController`, DTOs, `ApiMapper`, non-leaking `GlobalExceptionHandler`).

> Note: the tenant is currently taken from an `X-Tenant-Id` header. Step 13 replaces this with the
> JWT-derived tenant so it cannot be spoofed.

## Build, test, run

- Tests (no Docker needed — they use in-memory H2 under the `test` profile):
  ```bash
  ./mvnw test
  ```
- Full run against Postgres (needs Docker running):
  ```bash
  docker compose up --build
  ```
  API is then on `http://localhost:8080`. Target is **Java 21**.

## Conventions

- Commit messages are plain and human (imperative subject). No tool/attribution footers.
- Commit frequently with small, scoped commits; keep each step's tests green before committing.
- Keep the `core` packages framework-free (no Spring/JPA/HTTP) so the engine stays unit-testable.

## Remaining steps

### 13 — Auth, role-based access, tenant isolation (JWT)
Planned approach:
- Deps: `spring-boot-starter-security`, `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6`.
- `distance.auth` config: `secret` (>= 32 bytes, env-overridable), `token-ttl-minutes`, demo `users`
  (`username`, `password`, `tenant-id`, `role`). Bind with an `AuthProperties` record.
- `Role { EMPLOYEE, OPERATOR, ADMIN }`; `AuthenticatedUser(userId, tenantId, role)`.
- `JwtService` (HMAC-SHA256) issue/parse; `JwtAuthenticationFilter` sets a `ROLE_*` authority and
  the `AuthenticatedUser` principal; `SecurityConfig` stateless: permit `/api/auth/**` and
  `/actuator/health`; `POST /api/trips/**` → `OPERATOR`/`ADMIN`; `GET /api/trips/**` → any role.
- `AuthController` `POST /api/auth/login` validates a configured user and returns a token.
- Change `TripController` to read the tenant from `@AuthenticationPrincipal AuthenticatedUser`
  instead of the `X-Tenant-Id` header. Update `TripControllerTest` to mint a token via `JwtService`
  and send `Authorization: Bearer`; add tests for 401 (no token), 403 (employee POST), and
  cross-tenant isolation (a tenantB token gets 404 on a tenantA trip).

### 14 — Metrics & structured logging
- Micrometer counters/gauges: provider calls + cache-hit ratio (read from `CachingRoutingProvider`),
  degraded trips, recompute deltas, rejections by rule, compute latency timer, circuit state.
- Structured logs at service boundaries; **never log home location, phone or email**.
- Already exposed via `/actuator/prometheus`.

### 15 — Docker Compose end-to-end
- Confirm `docker compose up` builds and serves against Postgres (needs a running Docker daemon;
  not available on the original dev machine, so verify in an environment that has one).
- Add a `@SpringBootTest` context-load test (H2) if not already covered.

### 16 — Concurrency & failure-injection tests
- Concurrency: many threads computing/recomputing the same trip stay idempotent with no duplicate
  deltas; concurrent `CachingRoutingProvider` calls keep correct call counts.
- Failure injection: a provider that fails partway through a batch (add a `FlakyRoutingProvider`)
  → affected trips go `DEGRADED`, the rest succeed, and recompute after recovery reconciles.

### 17 — Benchmark
- `BenchmarkRunner` (CLI `main`): generate ~10,000 synthetic trips × ~1,500 pings, run the engine,
  report run time, memory, provider calls **before vs after** caching, and p50/p99 compute latency.
  Keep a scaled-down `@Tag("benchmark")` test for CI. Record results + the machine spec in the README.

### 18 — README & demo
- Flesh out the README: run instructions, auth/login + sample `curl` calls for each endpoint,
  benchmark numbers, trade-offs summary. Add a short demo script and (optionally) a demo video.
