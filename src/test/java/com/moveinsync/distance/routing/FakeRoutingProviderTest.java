package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.distance.Haversine;
import com.moveinsync.distance.core.model.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeRoutingProviderTest {

    private final FakeRoutingProvider provider = new FakeRoutingProvider();
    private final GeoPoint a = new GeoPoint(12.9716, 77.5946);
    private final GeoPoint b = new GeoPoint(13.0827, 80.2707);

    @Test
    void isDeterministic() {
        assertThat(provider.driveMeters(a, b)).isEqualTo(provider.driveMeters(a, b));
    }

    @Test
    void drivingDistanceIsAtLeastStraightLine() {
        assertThat(provider.driveMeters(a, b)).isGreaterThanOrEqualTo(Math.round(Haversine.meters(a, b)));
    }

    @Test
    void samePointIsZero() {
        assertThat(provider.driveMeters(a, a)).isZero();
    }

    @Test
    void appliesTheRoadFactor() {
        FakeRoutingProvider straight = new FakeRoutingProvider(1.0);
        assertThat(straight.driveMeters(a, b)).isEqualTo(Math.round(Haversine.meters(a, b)));
    }

    @Test
    void rejectsRoadFactorBelowOne() {
        assertThatThrownBy(() -> new FakeRoutingProvider(0.9))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
