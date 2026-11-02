package com.moveinsync.distance.service;

import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.DistanceDelta;
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
        e.setLatestResultStatus(ResultStatus.DEGRADED);
        trips.save(e);

        int recovered = job.recoverDegradedTrips();

        assertThat(recovered).isEqualTo(1);
        assertThat(service.result("tenantA", "recover-1").status()).isEqualTo(ResultStatus.OK);
    }

    @Test
    void aBilledDegradedTripIsCorrectedByADeltaExactlyOnce() {
        // A trip billed on a degraded estimate that is 1 km short of the real distance.
        service.compute("tenantA", new TripInputs("recover-billed",
                List.of(new Ping(new GeoPoint(12.9000, 77.6000), T0, 10),
                        new Ping(new GeoPoint(12.9450, 77.6000), T0.plusSeconds(600), 10)),
                List.of(), null, null, null, null));
        TripEntity e = trips.findByTripIdAndTenantId("recover-billed", "tenantA").orElseThrow();
        long realTotal = e.getTotalMeters();
        long estimate = realTotal - 1_000;
        e.setTotalMeters(estimate);
        e.setResultStatus(ResultStatus.DEGRADED);
        e.setLatestResultStatus(ResultStatus.DEGRADED);
        trips.save(e);
        service.bill("tenantA", "recover-billed");

        int recovered = job.recoverDegradedTrips();
        int secondRun = job.recoverDegradedTrips();

        assertThat(recovered).isEqualTo(1);
        assertThat(secondRun).isZero();
        List<DistanceDelta> ledger = service.ledger("tenantA", "recover-billed");
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).deltaMeters()).isEqualTo(1_000);
        assertThat(ledger.get(0).reason()).isEqualTo("provider-recovered");
        // The invoice itself is untouched.
        assertThat(service.result("tenantA", "recover-billed").totalMeters()).isEqualTo(estimate);
    }
}
