package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.SegmentRule;
import com.moveinsync.distance.core.model.TimelinePoint;
import com.moveinsync.distance.routing.CachingRoutingProvider;
import com.moveinsync.distance.routing.FakeRoutingProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineBatchTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static TimelinePoint point(double lat, double lon, long seconds) {
        return new TimelinePoint(new GeoPoint(lat, lon), T0.plusSeconds(seconds), null);
    }

    @Test
    void routesAllGapsOfATripInOneBatchRoundTrip() {
        CachingRoutingProvider cache = new CachingRoutingProvider(new FakeRoutingProvider(1.3), 3);
        RuleEngine engine = new RuleEngine(cache, RuleConfig.defaults());

        // Two real gaps in one timeline (two long hops).
        List<Segment> segments = engine.segment(List.of(
                point(12.9000, 77.6000, 0),
                point(12.9450, 77.6000, 600),
                point(12.9900, 77.6000, 1200)));

        long routed = segments.stream().filter(s -> s.rule() == SegmentRule.ROUTED).count();
        assertThat(routed).isEqualTo(2);
        // Both gaps were resolved in a single batched round-trip, not one call each.
        assertThat(cache.batchCalls()).isEqualTo(1);
        assertThat(cache.delegateCalls()).isEqualTo(2);
    }
}
