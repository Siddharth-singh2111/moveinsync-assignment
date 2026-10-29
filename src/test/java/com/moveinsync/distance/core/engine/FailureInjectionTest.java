package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.anchor.AnchorBuilder;
import com.moveinsync.distance.core.clean.StreamCleaner;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.routing.RoutingProvider;
import com.moveinsync.distance.core.rule.RuleConfig;
import com.moveinsync.distance.core.rule.RuleEngine;
import com.moveinsync.distance.routing.FailingRoutingProvider;
import com.moveinsync.distance.routing.FakeRoutingProvider;
import com.moveinsync.distance.routing.FlakyRoutingProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FailureInjectionTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private DistanceEngine engineWith(RoutingProvider provider) {
        return new DistanceEngine(
                new StreamCleaner(50.0, 120.0),
                new AnchorBuilder(true, 400.0),
                new RuleEngine(provider, RuleConfig.defaults()),
                Clock.fixed(T0, ZoneOffset.UTC));
    }

    private TripInputs realGapTrip(String id) {
        return new TripInputs(id,
                List.of(new Ping(new GeoPoint(12.9000, 77.6000), T0, 10),
                        new Ping(new GeoPoint(12.9450, 77.6000), T0.plusSeconds(600), 10)),
                List.of(), null, null, null, null);
    }

    @Test
    void providerFailingHalfwayDegradesOnlyTheAffectedTrips() {
        int trips = 10;
        int failFirst = 5; // one routing call per trip
        DistanceEngine engine = engineWith(new FlakyRoutingProvider(new FakeRoutingProvider(1.3), failFirst));

        int degraded = 0;
        int ok = 0;
        for (int i = 0; i < trips; i++) {
            DistanceResult r = engine.compute(realGapTrip("t-" + i));
            if (r.status() == ResultStatus.DEGRADED) {
                degraded++;
            } else {
                ok++;
            }
        }

        assertThat(degraded).isEqualTo(failFirst);
        assertThat(ok).isEqualTo(trips - failFirst);
    }

    @Test
    void degradedTripReconcilesAfterRecovery() {
        DistanceResult degraded = engineWith(new FailingRoutingProvider()).compute(realGapTrip("t-rec"));
        assertThat(degraded.status()).isEqualTo(ResultStatus.DEGRADED);

        DistanceResult recovered = engineWith(new FakeRoutingProvider(1.3)).compute(realGapTrip("t-rec"));
        assertThat(recovered.status()).isEqualTo(ResultStatus.OK);
        assertThat(recovered.totalMeters()).isNotEqualTo(degraded.totalMeters());
        assertThat(Deltas.between(degraded, recovered, "provider-recovered", T0)).isPresent();
    }
}
