package com.moveinsync.distance.metrics;

import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.service.TripService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DistanceMetricsTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TripService service;

    @Autowired
    private MeterRegistry registry;

    @Test
    void computingATripRecordsMeters() {
        double before = registry.get("distance.trips.computed").counter().count();

        service.compute("tenantA", new TripInputs("metrics-1",
                List.of(new Ping(new GeoPoint(12.9000, 77.6000), T0, 10),
                        new Ping(new GeoPoint(12.9450, 77.6000), T0.plusSeconds(600), 10)),
                List.of(), null, null, null, null));

        assertThat(registry.get("distance.trips.computed").counter().count()).isEqualTo(before + 1);
        assertThat(registry.get("distance.compute.latency").timer().count()).isGreaterThan(0);
        assertThat(registry.get("distance.provider.calls").gauge().value()).isGreaterThanOrEqualTo(0);
    }
}
