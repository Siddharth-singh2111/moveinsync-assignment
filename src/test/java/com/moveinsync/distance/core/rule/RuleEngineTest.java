package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.distance.Distances;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.SegmentRule;
import com.moveinsync.distance.core.model.TimelinePoint;
import com.moveinsync.distance.routing.FakeRoutingProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private final RuleEngine engine = new RuleEngine(new FakeRoutingProvider(1.3), RuleConfig.defaults());

    private static TimelinePoint point(double lat, double lon, long seconds) {
        return new TimelinePoint(new GeoPoint(lat, lon), T0.plusSeconds(seconds), null);
    }

    @Test
    void fewerThanTwoPointsProducesNoSegments() {
        assertThat(engine.segment(List.of())).isEmpty();
        assertThat(engine.segment(List.of(point(12.9, 77.6, 0)))).isEmpty();
    }

    @Test
    void shortGapUsesAerialWithNoProviderCall() {
        List<Segment> segments = engine.segment(List.of(
                point(12.9000, 77.6000, 0),
                point(12.9010, 77.6000, 10)));   // ~111 m in 10 s
        assertThat(segments).singleElement().satisfies(s -> {
            assertThat(s.rule()).isEqualTo(SegmentRule.SHORT_GAP_AERIAL);
            assertThat(s.providerCall()).isFalse();
        });
    }

    @Test
    void longSlowGapIsParked() {
        List<Segment> segments = engine.segment(List.of(
                point(12.90000, 77.6000, 0),
                point(12.90001, 77.6000, 600)));  // ~1 m in 600 s
        assertThat(segments).singleElement().satisfies(s -> {
            assertThat(s.rule()).isEqualTo(SegmentRule.PARKED);
            assertThat(s.providerCall()).isFalse();
        });
    }

    @Test
    void realGapIsRouted() {
        GeoPoint a = new GeoPoint(12.9000, 77.6000);
        GeoPoint b = new GeoPoint(12.9450, 77.6000);  // ~5 km in 600 s -> ~30 km/h
        List<Segment> segments = engine.segment(List.of(
                new TimelinePoint(a, T0, null),
                new TimelinePoint(b, T0.plusSeconds(600), null)));
        assertThat(segments).singleElement().satisfies(s -> {
            assertThat(s.rule()).isEqualTo(SegmentRule.ROUTED);
            assertThat(s.providerCall()).isTrue();
            assertThat(s.meters()).isGreaterThan(Distances.aerialMeters(a, b));
        });
    }

    @Test
    void routedDistanceOverCapIsRejected() {
        GeoPoint a = new GeoPoint(12.0000, 77.6000);
        GeoPoint b = new GeoPoint(12.7200, 77.6000);  // ~80 km; routed ~104 km > 100 km cap
        List<Segment> segments = engine.segment(List.of(
                new TimelinePoint(a, T0, null),
                new TimelinePoint(b, T0.plusSeconds(7200), null)));
        assertThat(segments).singleElement().satisfies(s -> {
            assertThat(s.rule()).isEqualTo(SegmentRule.REJECTED_SPEED);
            assertThat(s.meters()).isEqualTo(Distances.aerialMeters(a, b));
        });
    }
}
