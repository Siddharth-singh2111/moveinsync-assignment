package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutingProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caches driving distances to spare the real provider, which is slow, rate-limited and paid.
 * Each endpoint is snapped to a grid cell (coordinates rounded to {@code snapDecimals} places)
 * so that nearby gap endpoints collapse to the same key and reuse one call.
 *
 * <p>Tracks {@link #delegateCalls()} and {@link #cacheHits()} — the "provider calls before vs
 * after caching" number. A failed delegate call is propagated and never cached, so a transient
 * outage cannot poison the cache. Thread-safe.
 */
public final class CachingRoutingProvider implements RoutingProvider {

    private final RoutingProvider delegate;
    private final double snapFactor;
    private final Map<CellPair, Long> cache = new ConcurrentHashMap<>();
    private final AtomicLong delegateCalls = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();

    public CachingRoutingProvider(RoutingProvider delegate, int snapDecimals) {
        if (snapDecimals < 0) {
            throw new IllegalArgumentException("snapDecimals must be >= 0");
        }
        this.delegate = delegate;
        this.snapFactor = Math.pow(10, snapDecimals);
    }

    @Override
    public long driveMeters(GeoPoint from, GeoPoint to) {
        CellPair key = new CellPair(snap(from.latitude()), snap(from.longitude()),
                snap(to.latitude()), snap(to.longitude()));
        Long cached = cache.get(key);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }
        delegateCalls.incrementAndGet();
        long value = delegate.driveMeters(from, to); // throws -> not cached
        cache.put(key, value);
        return value;
    }

    /** Number of calls that actually reached the underlying provider. */
    public long delegateCalls() {
        return delegateCalls.get();
    }

    /** Number of calls served from the cache. */
    public long cacheHits() {
        return cacheHits.get();
    }

    /** Distinct cells currently cached. */
    public int cacheSize() {
        return cache.size();
    }

    private long snap(double coordinate) {
        return Math.round(coordinate * snapFactor);
    }

    private record CellPair(long fromLat, long fromLon, long toLat, long toLon) {
    }
}
