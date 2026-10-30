package com.moveinsync.distance.core.routing;

import com.moveinsync.distance.core.model.GeoPoint;

import java.util.List;

/**
 * The port the engine depends on for driving distance across a real gap. Implementations may
 * be slow, rate-limited or paid; the caching and circuit-breaker adapters wrap this same
 * interface. A call that cannot be served throws {@link RoutingException}.
 */
public interface RoutingProvider {

    /** Driving distance in metres between two points. Throws {@link RoutingException} on failure. */
    long driveMeters(GeoPoint from, GeoPoint to);

    /**
     * Resolve several routes at once, returned in the same order as {@code pairs}. The default
     * loops over single calls; the caching adapter overrides this to serve hits from cache and
     * fetch the misses in one batched round-trip.
     */
    default long[] driveMeters(List<RoutePair> pairs) {
        long[] out = new long[pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
            out[i] = driveMeters(pairs.get(i).from(), pairs.get(i).to());
        }
        return out;
    }
}
