package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.anchor.AnchorBuilder;
import com.moveinsync.distance.core.clean.StreamCleaner;
import com.moveinsync.distance.core.model.DistanceDelta;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.rule.RuleConfig;
import com.moveinsync.distance.core.rule.RuleEngine;
import com.moveinsync.distance.routing.FakeRoutingProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeltasTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static DistanceResult result(String tripId, long meters, String hash) {
        return new DistanceResult(tripId, meters, List.of(), 0, ResultStatus.OK, 1, hash, T0);
    }

    @Test
    void identicalTotalsProduceNoDelta() {
        DistanceResult a = result("t1", 30_000, "hashA");
        DistanceResult b = result("t1", 30_000, "hashA");
        assertThat(Deltas.unchanged(a, b)).isTrue();
        assertThat(Deltas.between(a, b, "recompute", T0)).isEmpty();
    }

    @Test
    void positiveDeltaWhenDistanceGrows() {
        DistanceResult before = result("t1", 30_000, "hashA");
        DistanceResult after = result("t1", 31_800, "hashB");
        Optional<DistanceDelta> delta = Deltas.between(before, after, "late pings", T0);
        assertThat(delta).isPresent();
        assertThat(delta.get().deltaMeters()).isEqualTo(1_800);
        assertThat(delta.get().deltaKm()).isEqualByComparingTo(new BigDecimal("1.800"));
        assertThat(delta.get().reason()).isEqualTo("late pings");
    }

    @Test
    void negativeDeltaWhenDistanceShrinks() {
        DistanceResult before = result("t1", 31_800, "hashB");
        DistanceResult after = result("t1", 30_000, "hashA");
        Optional<DistanceDelta> delta = Deltas.between(before, after, "correction", T0);
        assertThat(delta).isPresent();
        assertThat(delta.get().deltaMeters()).isEqualTo(-1_800);
        assertThat(delta.get().deltaKm()).isEqualByComparingTo(new BigDecimal("-1.800"));
    }

    @Test
    void refusesDeltaAcrossDifferentTrips() {
        assertThatThrownBy(() -> Deltas.between(result("t1", 10, "h"), result("t2", 20, "h"), "x", T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recomputeWithSameInputsIsIdempotent() {
        DistanceEngine engine = engine();
        TripInputs inputs = trip(List.of(
                new Ping(new GeoPoint(12.9000, 77.6000), T0, 10),
                new Ping(new GeoPoint(12.9450, 77.6000), T0.plusSeconds(600), 10)));
        DistanceResult first = engine.compute(inputs);
        DistanceResult second = engine.compute(inputs);
        assertThat(Deltas.unchanged(first, second)).isTrue();
        assertThat(Deltas.between(first, second, "recompute", T0)).isEmpty();
    }

    @Test
    void latePingsProduceASignedDelta() {
        DistanceEngine engine = engine();
        TripInputs before = trip(List.of(
                new Ping(new GeoPoint(12.9000, 77.6000), T0, 10),
                new Ping(new GeoPoint(12.9450, 77.6000), T0.plusSeconds(600), 10)));
        TripInputs after = trip(List.of(
                new Ping(new GeoPoint(12.9000, 77.6000), T0, 10),
                new Ping(new GeoPoint(12.9450, 77.6000), T0.plusSeconds(600), 10),
                new Ping(new GeoPoint(12.9900, 77.6000), T0.plusSeconds(1200), 10)));
        DistanceResult r1 = engine.compute(before);
        DistanceResult r2 = engine.compute(after);

        Optional<DistanceDelta> delta = Deltas.between(r1, r2, "late pings", T0);
        assertThat(delta).isPresent();
        assertThat(delta.get().deltaMeters()).isPositive();
    }

    private static DistanceEngine engine() {
        return new DistanceEngine(
                new StreamCleaner(50.0, 120.0),
                new AnchorBuilder(true, 400.0),
                new RuleEngine(new FakeRoutingProvider(1.3), RuleConfig.defaults()),
                Clock.fixed(T0, ZoneOffset.UTC));
    }

    private static TripInputs trip(List<Ping> pings) {
        return new TripInputs("t1", pings, List.of(), null, null, null, null);
    }
}
