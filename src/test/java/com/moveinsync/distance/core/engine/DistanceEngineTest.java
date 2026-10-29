package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.anchor.AnchorBuilder;
import com.moveinsync.distance.core.clean.StreamCleaner;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.SegmentRule;
import com.moveinsync.distance.core.rule.RuleConfig;
import com.moveinsync.distance.core.rule.RuleEngine;
import com.moveinsync.distance.routing.FakeRoutingProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceEngineTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final DistanceEngine engine = new DistanceEngine(
            new StreamCleaner(50.0, 120.0),
            new AnchorBuilder(true, 400.0),
            new RuleEngine(new FakeRoutingProvider(1.3), RuleConfig.defaults()),
            Clock.fixed(T0, ZoneOffset.UTC));

    private static Ping ping(double lat, double lon, long seconds) {
        return new Ping(new GeoPoint(lat, lon), T0.plusSeconds(seconds), 10);
    }

    private TripInputs trip(List<Ping> pings) {
        return new TripInputs("trip-1", pings, List.of(), null, null, null, null);
    }

    @Test
    void shortGapTripUsesNoProviderCalls() {
        DistanceResult result = engine.compute(trip(List.of(
                ping(12.9000, 77.6000, 0),
                ping(12.9010, 77.6000, 10),
                ping(12.9020, 77.6000, 20))));
        assertThat(result.providerCalls()).isZero();
        assertThat(result.status()).isEqualTo(ResultStatus.OK);
        assertThat(result.segments()).allSatisfy(
                s -> assertThat(s.rule()).isEqualTo(SegmentRule.SHORT_GAP_AERIAL));
    }

    @Test
    void realGapTripRoutesAndCountsCalls() {
        DistanceResult result = engine.compute(trip(List.of(
                ping(12.9000, 77.6000, 0),
                ping(12.9450, 77.6000, 600))));   // ~5 km in 600 s
        assertThat(result.providerCalls()).isEqualTo(1);
        assertThat(result.segments()).singleElement()
                .satisfies(s -> assertThat(s.rule()).isEqualTo(SegmentRule.ROUTED));
        assertThat(result.totalMeters()).isPositive();
    }

    @Test
    void computeIsDeterministic() {
        TripInputs inputs = trip(List.of(
                ping(12.9000, 77.6000, 0),
                ping(12.9450, 77.6000, 600),
                ping(12.9460, 77.6000, 610)));
        DistanceResult first = engine.compute(inputs);
        DistanceResult second = engine.compute(inputs);
        assertThat(second.totalMeters()).isEqualTo(first.totalMeters());
        assertThat(second.inputHash()).isEqualTo(first.inputHash());
        assertThat(second.segments()).isEqualTo(first.segments());
        assertThat(second.providerCalls()).isEqualTo(first.providerCalls());
    }

    @Test
    void differentInputsProduceDifferentHash() {
        DistanceResult a = engine.compute(trip(List.of(ping(12.9000, 77.6000, 0), ping(12.9450, 77.6000, 600))));
        DistanceResult b = engine.compute(trip(List.of(ping(12.9000, 77.6000, 0), ping(12.9460, 77.6000, 600))));
        assertThat(a.inputHash()).isNotEqualTo(b.inputHash());
    }
}
