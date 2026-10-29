package com.moveinsync.distance.benchmark;

import com.moveinsync.distance.core.anchor.AnchorBuilder;
import com.moveinsync.distance.core.clean.StreamCleaner;
import com.moveinsync.distance.core.engine.DistanceEngine;
import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.rule.RuleConfig;
import com.moveinsync.distance.core.rule.RuleEngine;
import com.moveinsync.distance.routing.CachingRoutingProvider;
import com.moveinsync.distance.routing.FakeRoutingProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Headless load test for the engine. Generates many synthetic trips of ~1,500 pings each and
 * reports run time, throughput, p50/p99 compute latency, heap used, and the routing-call count
 * before vs after caching.
 *
 * <p>Routed gaps hop between a small fixed pool of hub locations, so their snapped cells repeat
 * across trips — which is exactly the real-world pattern (cabs crossing the same underpasses) the
 * cache is meant to exploit.
 *
 * <p>Run: {@code ./run-benchmark.sh [trips] [pingsPerTrip]} (defaults 10000 and 1500).
 */
public final class BenchmarkRunner {

    private static final int HUBS = 16;
    private static final GeoPoint[] HUB = buildHubs();
    private static final Instant EPOCH = Instant.parse("2026-01-01T00:00:00Z");
    private static final int GAP_INTERVAL = 100;

    private BenchmarkRunner() {
    }

    public static void main(String[] args) {
        int trips = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
        int pingsPerTrip = args.length > 1 ? Integer.parseInt(args[1]) : 1_500;
        System.out.println(format(run(trips, pingsPerTrip)));
    }

    public record BenchmarkResult(int trips, int pingsPerTrip, long elapsedMillis, double throughputPerSec,
                                  double p50Millis, double p99Millis,
                                  long requestsBeforeCache, long requestsAfterCache, long heapUsedMb) {
    }

    public static BenchmarkResult run(int trips, int pingsPerTrip) {
        CachingRoutingProvider routing = new CachingRoutingProvider(new FakeRoutingProvider(1.3), 3);
        DistanceEngine engine = new DistanceEngine(
                new StreamCleaner(50.0, 120.0),
                new AnchorBuilder(true, 400.0),
                new RuleEngine(routing, RuleConfig.defaults()),
                Clock.systemUTC());

        long[] latencies = new long[trips];
        long start = System.nanoTime();
        for (int i = 0; i < trips; i++) {
            TripInputs inputs = syntheticTrip("bench-" + i, pingsPerTrip, i);
            long t0 = System.nanoTime();
            engine.compute(inputs);
            latencies[i] = System.nanoTime() - t0;
        }
        long elapsedNanos = System.nanoTime() - start;

        Arrays.sort(latencies);
        double p50 = latencies[(int) (trips * 0.50)] / 1_000_000.0;
        double p99 = latencies[Math.min(trips - 1, (int) (trips * 0.99))] / 1_000_000.0;

        Runtime rt = Runtime.getRuntime();
        long heapUsedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        long after = routing.delegateCalls();
        long before = after + routing.cacheHits();
        double throughput = trips / (elapsedNanos / 1_000_000_000.0);

        return new BenchmarkResult(trips, pingsPerTrip, elapsedNanos / 1_000_000, throughput,
                p50, p99, before, after, heapUsedMb);
    }

    private static TripInputs syntheticTrip(String id, int pings, int seed) {
        List<Ping> list = new ArrayList<>(pings);
        Instant time = EPOCH;
        int hub = seed % HUBS;
        GeoPoint current = HUB[hub];
        for (int j = 0; j < pings; j++) {
            if (j > 0 && j % GAP_INTERVAL == 0) {
                // A routed leg: hop to the next hub over a long gap.
                hub = (hub + 1) % HUBS;
                current = HUB[hub];
                time = time.plusSeconds(300);
            } else {
                // A short step around the current hub (cheap aerial segment).
                current = new GeoPoint(current.latitude() + 0.00001, current.longitude());
                time = time.plusSeconds(2);
            }
            list.add(new Ping(current, time, 10));
        }
        return new TripInputs(id, list, List.of(), null, null, null, null);
    }

    private static GeoPoint[] buildHubs() {
        GeoPoint[] hubs = new GeoPoint[HUBS];
        for (int i = 0; i < HUBS; i++) {
            hubs[i] = new GeoPoint(12.90 + (i / 4) * 0.05, 77.50 + (i % 4) * 0.05);
        }
        return hubs;
    }

    private static String format(BenchmarkResult r) {
        double reduction = r.requestsBeforeCache() == 0 ? 0
                : 100.0 * (r.requestsBeforeCache() - r.requestsAfterCache()) / r.requestsBeforeCache();
        return """
                === GPS distance benchmark ===
                trips                : %d
                pings/trip           : %d
                elapsed              : %d ms
                throughput           : %.0f trips/s
                compute p50 / p99    : %.3f ms / %.3f ms
                routing calls before : %d
                routing calls after  : %d  (%.1f%% fewer with caching)
                heap used            : %d MB
                """.formatted(r.trips(), r.pingsPerTrip(), r.elapsedMillis(), r.throughputPerSec(),
                r.p50Millis(), r.p99Millis(), r.requestsBeforeCache(), r.requestsAfterCache(),
                reduction, r.heapUsedMb());
    }
}
