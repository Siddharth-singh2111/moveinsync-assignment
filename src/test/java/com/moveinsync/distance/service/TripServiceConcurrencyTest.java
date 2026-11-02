package com.moveinsync.distance.service;

import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.DistanceDelta;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent writes to one billed trip, committed for real so the row lock is what keeps them
 * correct. Deliberately not {@code @Transactional}: a shared test transaction would hide the race.
 */
@SpringBootTest
@ActiveProfiles("test")
class TripServiceConcurrencyTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final int THREADS = 8;

    @Autowired
    private TripService service;

    private static Ping ping(double lat, long seconds) {
        return new Ping(new GeoPoint(lat, 77.6000), T0.plusSeconds(seconds), 10);
    }

    private void billedTrip(String tripId) {
        service.compute("tenantA", new TripInputs(tripId, List.of(ping(12.9000, 0), ping(12.9450, 600)),
                List.of(), null, null, null, null));
        service.bill("tenantA", tripId);
    }

    @Test
    void theSameLatePingFromManyThreadsAppendsOneDelta() throws Exception {
        billedTrip("c-same");
        List<List<Ping>> deliveries = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            deliveries.add(List.of(ping(12.9900, 1200)));
        }

        ingestConcurrently("c-same", deliveries);

        assertThat(service.ledger("tenantA", "c-same")).hasSize(1);
    }

    @Test
    void differentLatePingsFromManyThreadsAreAllKeptAndReconcile() throws Exception {
        billedTrip("c-many");
        long billedTotal = service.result("tenantA", "c-many").totalMeters();
        List<List<Ping>> deliveries = new ArrayList<>();
        for (int i = 1; i <= THREADS; i++) {
            deliveries.add(List.of(ping(12.9450 + i * 0.045, 600 + i * 600L)));
        }

        ingestConcurrently("c-many", deliveries);

        List<Ping> all = new ArrayList<>(List.of(ping(12.9000, 0), ping(12.9450, 600)));
        deliveries.forEach(all::addAll);
        service.compute("tenantCheck", new TripInputs("c-many", all, List.of(), null, null, null, null));
        long trueTotal = service.result("tenantCheck", "c-many").totalMeters();
        long ledgerTotal = service.ledger("tenantA", "c-many").stream().mapToLong(DistanceDelta::deltaMeters).sum();
        assertThat(billedTotal + ledgerTotal).isEqualTo(trueTotal);
    }

    private void ingestConcurrently(String tripId, List<List<Ping>> deliveries) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(deliveries.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<TripComputation>> results = new ArrayList<>();
            for (List<Ping> pings : deliveries) {
                results.add(pool.submit(() -> {
                    start.await();
                    return service.ingestPings("tenantA", tripId, pings);
                }));
            }
            start.countDown();
            for (Future<TripComputation> result : results) {
                result.get();
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
