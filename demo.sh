#!/usr/bin/env bash
# End-to-end demo against a running service (docker compose up).
# Walks through: login -> compute -> explain -> bill -> late pings (delta) -> ledger.
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"

extract() { sed -E "s/.*\"$1\":\"?([^\",}]+)\"?.*/\1/"; }

echo "== login =="
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"operator","password":"operator-pass"}')
echo "$LOGIN"
TOKEN=$(echo "$LOGIN" | extract token)
AUTH="Authorization: Bearer $TOKEN"

echo; echo "== compute trip demo-1 (one real gap) =="
curl -s -X POST "$BASE/api/trips" -H "$AUTH" -H 'Content-Type: application/json' -d '{
  "tripId":"demo-1",
  "pings":[
    {"lat":12.9000,"lon":77.6000,"timestamp":"2026-01-01T00:00:00Z","accuracyMeters":10},
    {"lat":12.9450,"lon":77.6000,"timestamp":"2026-01-01T00:10:00Z","accuracyMeters":10}
  ],
  "milestones":[],"riders":[]
}'
echo

echo; echo "== explain (per-segment rules) =="
curl -s "$BASE/api/trips/demo-1/explain" -H "$AUTH"; echo

echo; echo "== bill the trip =="
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BASE/api/trips/demo-1/bill" -H "$AUTH"

echo; echo "== late pings arrive after billing (appends a signed delta) =="
curl -s -X POST "$BASE/api/trips/demo-1/pings" -H "$AUTH" -H 'Content-Type: application/json' -d '{
  "pings":[{"lat":12.9900,"lon":77.6000,"timestamp":"2026-01-01T00:20:00Z","accuracyMeters":10}]
}'
echo

echo; echo "== delta ledger =="
curl -s "$BASE/api/trips/demo-1/deltas" -H "$AUTH"; echo
