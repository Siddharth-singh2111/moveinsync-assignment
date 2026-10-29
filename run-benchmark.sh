#!/usr/bin/env bash
# Run the engine benchmark. Usage: ./run-benchmark.sh [trips] [pingsPerTrip]
# Defaults to 10000 trips of 1500 pings each.
set -euo pipefail

TRIPS="${1:-10000}"
PINGS="${2:-1500}"

./mvnw -q compile exec:java -Dexec.args="${TRIPS} ${PINGS}"
