# API walkthrough for the demo

Two ways to show the API on camera. Both tell the same story and end on the
signed-delta ledger, which is the most convincing thing to demonstrate.

Start the service first:

```bash
docker compose up --build
```

## Option A — Postman (looks best on camera)

1. Open Postman → **Import** → drop in
   [`gps-distance.postman_collection.json`](gps-distance.postman_collection.json).
2. Run the requests **top to bottom** (1 → 8). Request **1 · Login** saves the JWT into the
   `{{token}}` collection variable automatically, so every later request is already authorized —
   you never copy-paste a token on camera.
3. `baseUrl` defaults to `http://localhost:8080`; change the collection variable if yours differs.

What each step shows:

| # | Request | What to say |
|---|---|---|
| 1 | Login | "I authenticate as an operator and get a JWT — the tenant is baked into the token." |
| 2 | Compute | "Two pings with a 10-minute gap — the engine routes that one real gap. `providerCalls: 1`." |
| 3 | Distance | "The billable total, its status, and the rule version it was computed under." |
| 4 | Explain | "Every segment labelled with the rule that produced it — this is the dispute view." |
| 5 | Bill | "Close the invoice. 204. From here the number is frozen." |
| 6 | Late pings | "A ping arrives *after* billing. The billed total is untouched — instead I get a signed delta." |
| 7 | Deltas | "The append-only ledger. We never rewrite a closed invoice; we reconcile with deltas." |
| 8 | 401 (optional) | "Same call with no token = 401. Tenant isolation and auth are real." |

## Option B — curl (paste one line at a time)

```bash
# 1. Login and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"operator","password":"operator-pass"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
echo "$TOKEN"

# 2. Compute a trip with one real gap
curl -s -X POST http://localhost:8080/api/trips \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
    "tripId":"demo-1",
    "pings":[
      {"lat":12.9000,"lon":77.6000,"timestamp":"2026-01-01T00:00:00Z","accuracyMeters":10},
      {"lat":12.9450,"lon":77.6000,"timestamp":"2026-01-01T00:10:00Z","accuracyMeters":10}
    ],
    "milestones":[],"riders":[]
  }'

# 3. Distance
curl -s http://localhost:8080/api/trips/demo-1/distance -H "Authorization: Bearer $TOKEN"

# 4. Explain (per-segment rules)
curl -s http://localhost:8080/api/trips/demo-1/explain -H "Authorization: Bearer $TOKEN"

# 5. Bill (freeze the invoice) — prints the HTTP status (204)
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/trips/demo-1/bill \
  -H "Authorization: Bearer $TOKEN"

# 6. Late ping after billing -> signed delta in the response
curl -s -X POST http://localhost:8080/api/trips/demo-1/pings \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"pings":[{"lat":12.9900,"lon":77.6000,"timestamp":"2026-01-01T00:20:00Z","accuracyMeters":10}]}'

# 7. The delta ledger
curl -s http://localhost:8080/api/trips/demo-1/deltas -H "Authorization: Bearer $TOKEN"

# 8. (optional) no token = 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/trips/demo-1/distance
```

> Tip: pipe any response through `| jq` for pretty-printed JSON on screen
> (`curl ... | jq`).

The all-in-one version of this flow is [`../demo.sh`](../demo.sh) — handy as a dry run before you
record, but on camera the step-by-step calls above read more clearly.
