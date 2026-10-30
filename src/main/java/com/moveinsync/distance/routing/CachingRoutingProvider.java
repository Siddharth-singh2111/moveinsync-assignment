package com.moveinsync.distance.routing;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutePair;
import com.moveinsync.distance.core.routing.RoutingProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caches driving distances to spare the real provider, which is slow, rate-limited and paid.
 * Each endpoint is snapped to a grid cell (coordinates rounded to {@code snapDecimals} places)
 * so nearby gap endpoints collapse to the same key and reuse one call.
 *
 * <p>Backed by a <b>Caffeine</b> cache with a bounded size and a write TTL, so the cache cannot
 * grow without limit and an entry cannot outlive its freshness window. <b>What can go stale:</b> a
 * cached driving distance if a road genuinely changes. <b>For how long:</b> at most the TTL
 * (default 7 days). <b>How bounded:</b> least-recently-used eviction at {@code maxSize}, so memory
 * is capped regardless of traffic.
 *
 * <p>Tracks {@link #delegateCalls()}, {@link #cacheHits()} and {@link #batchCalls()}. A failed
 * delegate call is propagated and never cached, so a transient outage cannot poison the cache.
 */
public final class CachingRoutingProvider implements RoutingProvider {

    private final RoutingProvider delegate;
    private final double snapFactor;
    private final Cache<CellPair, Long> cache;
    private final AtomicLong delegateCalls = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong batchCalls = new AtomicLong();

    public CachingRoutingProvider(RoutingProvider delegate, int snapDecimals) {
        this(delegate, snapDecimals, 100_000, Duration.ofDays(7));
    }

    public CachingRoutingProvider(RoutingProvider delegate, int snapDecimals, long maxSize, Duration ttl) {
        if (snapDecimals < 0) {
            throw new IllegalArgumentException("snapDecimals must be >= 0");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }
        this.delegate = delegate;
        this.snapFactor = Math.pow(10, snapDecimals);
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Objects.requireNonNull(ttl, "ttl"))
                .build();
    }

    @Override
    public long driveMeters(GeoPoint from, GeoPoint to) {
        CellPair key = key(from, to);
        Long cached = cache.getIfPresent(key);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }
        delegateCalls.incrementAndGet();
        long value = delegate.driveMeters(from, to); // throws -> not cached
        cache.put(key, value);
        return value;
    }

    /**
     * Resolve many routes with cache reuse and a single batched round-trip to the provider for
     * the ones that miss — so a trip's real gaps cost one request, not one per gap.
     */
    @Override
    public long[] driveMeters(List<RoutePair> pairs) {
        long[] out = new long[pairs.size()];
        List<Integer> missIndexes = new ArrayList<>();
        List<RoutePair> misses = new ArrayList<>();

        for (int i = 0; i < pairs.size(); i++) {
            CellPair key = key(pairs.get(i).from(), pairs.get(i).to());
            Long cached = cache.getIfPresent(key);
            if (cached != null) {
                cacheHits.incrementAndGet();
                out[i] = cached;
            } else {
                missIndexes.add(i);
                misses.add(pairs.get(i));
            }
        }

        if (!misses.isEmpty()) {
            batchCalls.incrementAndGet();
            long[] fetched = delegate.driveMeters(misses); // one batched round-trip; throws -> nothing cached
            for (int j = 0; j < misses.size(); j++) {
                delegateCalls.incrementAndGet();
                cache.put(key(misses.get(j).from(), misses.get(j).to()), fetched[j]);
                out[missIndexes.get(j)] = fetched[j];
            }
        }
        return out;
    }

    /** Calls that actually reached the underlying provider (routes fetched). */
    public long delegateCalls() {
        return delegateCalls.get();
    }

    /** Calls served from the cache. */
    public long cacheHits() {
        return cacheHits.get();
    }

    /** Batched round-trips made to the provider. */
    public long batchCalls() {
        return batchCalls.get();
    }

    /** Approximate number of cells currently cached (bounded by maxSize). */
    public long cacheSize() {
        return cache.estimatedSize();
    }

    /** Force pending eviction/expiry maintenance (useful in tests and for diagnostics). */
    public void cleanUp() {
        cache.cleanUp();
    }

    private CellPair key(GeoPoint from, GeoPoint to) {
        return new CellPair(snap(from.latitude()), snap(from.longitude()),
                snap(to.latitude()), snap(to.longitude()));
    }

    private long snap(double coordinate) {
        return Math.round(coordinate * snapFactor);
    }

    private record CellPair(long fromLat, long fromLon, long toLat, long toLon) {
    }
}
