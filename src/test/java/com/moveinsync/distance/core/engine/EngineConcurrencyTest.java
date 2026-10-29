package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.anchor.AnchorBuilder;
import com.moveinsync.distance.core.clean.StreamCleaner;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.rule.RuleConfig;
import com.moveinsync.distance.core.rule.RuleEngine;
import com.moveinsync.distance.routing.FakeRoutingProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class EngineConcurrencyTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final DistanceEngine engine = new DistanceEngine(
            new StreamCleaner(50.0, 120.0),
            new AnchorBuilder(true, 400.0),
            new RuleEngine(new FakeRoutingProvider(1.3), RuleConfig.defaults()),
            Clock.fixed(T0, ZoneOffset.UTC));

    @Test
    void sameInputsAcrossManyThreadsGiveIdenticalResults() throws Exception {
        TripInputs inputs = new TripInputs("concurrent",
                List.of(new Ping(new GeoPoint(12.9000, 77.6000), T0, 10),
                        new Ping(new GeoPoint(12.9450, 77.6000), T0.plusSeconds(600), 10),
                        new Ping(new GeoPoint(12.9460, 77.6000), T0.plusSeconds(610), 10)),
                List.of(), null, null, null, null);

        int tasks = 64;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<DistanceResult>> jobs = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                jobs.add(() -> engine.compute(inputs));
            }
            List<Future<DistanceResult>> futures = pool.invokeAll(jobs);

            DistanceResult first = futures.get(0).get();
            for (Future<DistanceResult> f : futures) {
                DistanceResult r = f.get();
                assertThat(r.totalMeters()).isEqualTo(first.totalMeters());
                assertThat(r.inputHash()).isEqualTo(first.inputHash());
                assertThat(r.segments()).isEqualTo(first.segments());
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
