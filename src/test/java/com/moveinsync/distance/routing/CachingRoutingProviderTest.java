package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutingException;
import com.moveinsync.distance.core.routing.RoutingProvider;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachingRoutingProviderTest {

    private final GeoPoint a = new GeoPoint(12.9000, 77.6000);
    private final GeoPoint b = new GeoPoint(12.9500, 77.6500);

    @Test
    void repeatedIdenticalGapCallsDelegateOnce() {
        AtomicInteger calls = new AtomicInteger();
        RoutingProvider counting = (from, to) -> {
            calls.incrementAndGet();
            return 1000L;
        };
        CachingRoutingProvider cache = new CachingRoutingProvider(counting, 3);

        assertThat(cache.driveMeters(a, b)).isEqualTo(1000L);
        assertThat(cache.driveMeters(a, b)).isEqualTo(1000L);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(cache.delegateCalls()).isEqualTo(1);
        assertThat(cache.cacheHits()).isEqualTo(1);
    }

    @Test
    void nearbyPointsInSameCellReuseTheCall() {
        AtomicInteger calls = new AtomicInteger();
        RoutingProvider counting = (from, to) -> {
            calls.incrementAndGet();
            return 1000L;
        };
        CachingRoutingProvider cache = new CachingRoutingProvider(counting, 3);

        cache.driveMeters(a, b);
        // Shift both endpoints by less than half a cell (< 0.0005 deg) -> same snapped cell.
        cache.driveMeters(new GeoPoint(12.90001, 77.60001), new GeoPoint(12.95001, 77.65001));

        assertThat(calls.get()).isEqualTo(1);
        assertThat(cache.cacheHits()).isEqualTo(1);
    }

    @Test
    void distinctGapsEachCallDelegate() {
        AtomicInteger calls = new AtomicInteger();
        RoutingProvider counting = (from, to) -> (long) calls.incrementAndGet();
        CachingRoutingProvider cache = new CachingRoutingProvider(counting, 3);

        cache.driveMeters(a, b);
        cache.driveMeters(a, new GeoPoint(13.1000, 77.9000));

        assertThat(cache.delegateCalls()).isEqualTo(2);
        assertThat(cache.cacheSize()).isEqualTo(2);
    }

    @Test
    void failuresAreNotCached() {
        AtomicInteger calls = new AtomicInteger();
        RoutingProvider failing = (from, to) -> {
            calls.incrementAndGet();
            throw new RoutingException("down");
        };
        CachingRoutingProvider cache = new CachingRoutingProvider(failing, 3);

        assertThatThrownBy(() -> cache.driveMeters(a, b)).isInstanceOf(RoutingException.class);
        assertThatThrownBy(() -> cache.driveMeters(a, b)).isInstanceOf(RoutingException.class);

        assertThat(calls.get()).isEqualTo(2);
        assertThat(cache.cacheSize()).isZero();
    }
}
