package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutePair;
import com.moveinsync.distance.core.routing.RoutingProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CachingRoutingProviderBatchEvictionTest {

    private final GeoPoint a = new GeoPoint(12.9000, 77.6000);
    private final GeoPoint b = new GeoPoint(12.9500, 77.6500);

    @Test
    void cacheIsBoundedByMaxSize() {
        RoutingProvider delegate = (from, to) -> 1000L;
        CachingRoutingProvider cache = new CachingRoutingProvider(delegate, 6, 10, Duration.ofDays(1));

        for (int i = 0; i < 100; i++) {
            cache.driveMeters(new GeoPoint(12.0 + i * 0.001, 77.0), new GeoPoint(13.0, 77.0));
        }
        cache.cleanUp();

        assertThat(cache.cacheSize()).isLessThanOrEqualTo(10);
    }

    @Test
    void batchServesCacheHitsAndFetchesMissesInOneRoundTrip() {
        AtomicInteger singleCalls = new AtomicInteger();
        AtomicInteger batchCalls = new AtomicInteger();
        RoutingProvider delegate = new RoutingProvider() {
            @Override
            public long driveMeters(GeoPoint from, GeoPoint to) {
                singleCalls.incrementAndGet();
                return 1000L;
            }

            @Override
            public long[] driveMeters(List<RoutePair> pairs) {
                batchCalls.incrementAndGet();
                long[] out = new long[pairs.size()];
                Arrays.fill(out, 2000L);
                return out;
            }
        };
        CachingRoutingProvider cache = new CachingRoutingProvider(delegate, 3);

        cache.driveMeters(a, b); // pre-cache (a,b) via a single call

        RoutePair cached = new RoutePair(a, b);
        RoutePair miss1 = new RoutePair(new GeoPoint(12.1, 77.1), new GeoPoint(12.2, 77.2));
        RoutePair miss2 = new RoutePair(new GeoPoint(12.3, 77.3), new GeoPoint(12.4, 77.4));

        long[] result = cache.driveMeters(List.of(cached, miss1, miss2));

        assertThat(result[0]).isEqualTo(1000L); // served from cache
        assertThat(result[1]).isEqualTo(2000L); // fetched in the batch
        assertThat(result[2]).isEqualTo(2000L);
        assertThat(singleCalls.get()).isEqualTo(1);
        assertThat(batchCalls.get()).isEqualTo(1);        // exactly one batched round-trip
        assertThat(cache.batchCalls()).isEqualTo(1);
    }
}
