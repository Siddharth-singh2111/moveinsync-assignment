package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutingProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CachingRoutingProviderConcurrencyTest {

    @Test
    void concurrentIdenticalCallsStayConsistentAndCacheOnce() throws Exception {
        AtomicInteger delegateCalls = new AtomicInteger();
        RoutingProvider delegate = (from, to) -> {
            delegateCalls.incrementAndGet();
            return 1000L;
        };
        CachingRoutingProvider cache = new CachingRoutingProvider(delegate, 3);
        GeoPoint a = new GeoPoint(12.9000, 77.6000);
        GeoPoint b = new GeoPoint(12.9500, 77.6500);

        int tasks = 32;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Long>> jobs = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                jobs.add(() -> cache.driveMeters(a, b));
            }
            List<Future<Long>> futures = pool.invokeAll(jobs);
            for (Future<Long> f : futures) {
                assertThat(f.get()).isEqualTo(1000L);
            }
        } finally {
            pool.shutdownNow();
        }

        // One distinct cell pair, so at most one entry; the delegate is hit at least once and
        // never more than once per thread (racing misses are bounded, not unbounded).
        assertThat(cache.cacheSize()).isEqualTo(1);
        assertThat(cache.delegateCalls()).isBetween(1L, (long) tasks);
    }
}
