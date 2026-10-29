package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.distance.Haversine;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutingProvider;

/**
 * A deterministic stand-in for a real routing API: driving distance is modelled as the
 * straight-line distance times a road factor (roads are longer than the crow flies). Being
 * deterministic, it keeps tests and the engine's determinism guarantee intact, and needs no
 * network or API key.
 */
public final class FakeRoutingProvider implements RoutingProvider {

    private final double roadFactor;

    public FakeRoutingProvider() {
        this(1.3);
    }

    public FakeRoutingProvider(double roadFactor) {
        if (roadFactor < 1.0) {
            throw new IllegalArgumentException("roadFactor must be >= 1.0");
        }
        this.roadFactor = roadFactor;
    }

    @Override
    public long driveMeters(GeoPoint from, GeoPoint to) {
        return Math.round(Haversine.meters(from, to) * roadFactor);
    }
}
