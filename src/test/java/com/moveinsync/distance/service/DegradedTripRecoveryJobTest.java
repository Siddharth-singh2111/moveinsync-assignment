package com.moveinsync.distance.service;

import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.persistence.TripEntity;
import com.moveinsync.distance.persistence.TripRepository;
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
class DegradedTripRecoveryJobTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TripService service;

    @Autowired
    private TripRepository trips;

    @Autowired
    private DegradedTripRecoveryJob job;

    @Test
    void recomputesDegradedTripsOnceProviderIsHealthy() {
        // Compute a trip, then force its stored result to DEGRADED to simulate a provider outage.
        service.compute("tenantA", new TripInputs("recover-1",
                List.of(new Ping(new GeoPoint(12.9000, 77.6000), T0, 10),
                        new Ping(new GeoPoint(12.9450, 77.6000), T0.plusSeconds(600), 10)),
                List.of(), null, null, null, null));
        TripEntity e = trips.findByTripIdAndTenantId("recover-1", "tenantA").orElseThrow();
        e.setResultStatus(ResultStatus.DEGRADED);
        trips.save(e);

        int recovered = job.recoverDegradedTrips();

        assertThat(recovered).isEqualTo(1);
        assertThat(service.result("tenantA", "recover-1").status()).isEqualTo(ResultStatus.OK);
    }
}
