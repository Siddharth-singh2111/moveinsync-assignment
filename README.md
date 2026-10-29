# GPS Gap Backfill & Billable Distance Reconstruction

Rebuilds a trusted, explainable billable distance for every cab trip from unreliable GPS
pings. Cheap maths where possible, trusted anchor points where needed, and a cached,
circuit-broken routing provider only for real gaps — with every segment labelled by the
rule that produced it.

See **[DESIGN.md](DESIGN.md)** for the full design note (architecture, the hardest-flow
sequence diagram, failure modes, and trade-offs).

## Run

```bash
docker compose up --build
```

This starts PostgreSQL and the service (built inside Docker, so only Docker is required).
The API is then available on `http://localhost:8080`.

### Build / test locally

```bash
./mvnw verify
```

Requires JDK 21.

## Package layout

```
com.moveinsync.distance
├── core            framework-free engine (no Spring, no DB, no HTTP)
│   ├── model         domain types: Ping, Anchor, Segment, DistanceResult, ...
│   ├── clean         StreamCleaner — dedupe, order, drop bad fixes
│   ├── anchor        AnchorBuilder — patch gaps with anchor points
│   ├── rule          RuleEngine + ordered SegmentRule strategies
│   ├── distance      haversine + DistanceAggregator
│   └── routing       RoutingProvider port (interface)
├── routing         routing adapters (cache + circuit breaker, fake/failing providers)
├── service         orchestration + idempotent recompute + signed deltas
├── persistence     JPA entities and repositories
├── api             REST controllers + DTOs + error handling
├── config          Spring, cache and resilience configuration
└── metrics         counters and timers
```

## Configuration

Domain rule thresholds live under `distance.rules` in
[`application.yml`](src/main/resources/application.yml) and are stamped onto every result
as `ruleVersion`, so two runs of the same inputs under the same version are byte-identical.
