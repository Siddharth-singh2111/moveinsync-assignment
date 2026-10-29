package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.anchor.AnchorBuilder;
import com.moveinsync.distance.core.clean.StreamCleaner;
import com.moveinsync.distance.core.distance.Distances;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.SegmentRule;
import com.moveinsync.distance.core.rule.RuleConfig;
import com.moveinsync.distance.core.rule.RuleEngine;
import com.moveinsync.distance.routing.FailingRoutingProvider;
import com.moveinsync.distance.routing.FakeRoutingProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceEngineDegradedTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final GeoPoint A = new GeoPoint(12.9000, 77.6000);
    private static final GeoPoint B = new GeoPoint(12.9450, 77.6000); // ~5 km, real gap

    private DistanceEngine engineWith(com.moveinsync.distance.core.routing.RoutingProvider provider) {
        return new DistanceEngine(
                new StreamCleaner(50.0, 120.0),
                new AnchorBuilder(true, 400.0),
                new RuleEngine(provider, RuleConfig.defaults()),
                Clock.fixed(T0, ZoneOffset.UTC));
    }

    private TripInputs realGapTrip() {
        return new TripInputs("trip-1",
                List.of(new Ping(A, T0, 10), new Ping(B, T0.plusSeconds(600), 10)),
                List.of(), null, null, null, null);
    }

    @Test
    void providerFailureDegradesTheTrip() {
        DistanceResult result = engineWith(new FailingRoutingProvider()).compute(realGapTrip());

        assertThat(result.status()).isEqualTo(ResultStatus.DEGRADED);
        assertThat(result.providerCalls()).isZero();
        assertThat(result.segments()).singleElement().satisfies(s -> {
            assertThat(s.rule()).isEqualTo(SegmentRule.DEGRADED_AERIAL);
            assertThat(s.meters()).isEqualTo(Math.round(Distances.aerialMeters(A, B) * 1.4));
        });
    }

    @Test
    void recomputeAfterRecoveryIsOk() {
        DistanceResult recovered = engineWith(new FakeRoutingProvider(1.3)).compute(realGapTrip());

        assertThat(recovered.status()).isEqualTo(ResultStatus.OK);
        assertThat(recovered.segments()).singleElement()
                .satisfies(s -> assertThat(s.rule()).isEqualTo(SegmentRule.ROUTED));
    }
}
