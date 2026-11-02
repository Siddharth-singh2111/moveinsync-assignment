package com.moveinsync.distance.persistence;

import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.TripStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TripRepositoryTest {

    @Autowired
    private TripRepository trips;

    private static TripEntity trip(String tripId, String tenantId) {
        TripEntity e = new TripEntity();
        e.setTripId(tripId);
        e.setTenantId(tenantId);
        e.setStatus(TripStatus.COMPUTED);
        e.setRuleVersion(1);
        e.setInputHash("hash");
        e.setTotalMeters(30_600);
        e.setProviderCalls(2);
        e.setResultStatus(ResultStatus.OK);
        e.setLatestResultStatus(ResultStatus.OK);
        e.setComputedAt(Instant.parse("2026-01-01T00:00:00Z"));
        e.setInputsJson("{}");
        e.setSegmentsJson("[]");
        return e;
    }

    @Test
    void findsTripWithinItsTenant() {
        trips.save(trip("t1", "tenantA"));
        assertThat(trips.findByTripIdAndTenantId("t1", "tenantA")).isPresent();
    }

    @Test
    void doesNotReturnTripForAnotherTenant() {
        trips.save(trip("t1", "tenantA"));
        assertThat(trips.findByTripIdAndTenantId("t1", "tenantB")).isEmpty();
    }

    @Test
    void twoTenantsCanUseTheSameTripId() {
        trips.saveAndFlush(trip("t1", "tenantA"));
        trips.saveAndFlush(trip("t1", "tenantB"));
        assertThat(trips.findByTripIdAndTenantId("t1", "tenantA")).isPresent();
        assertThat(trips.findByTripIdAndTenantId("t1", "tenantB")).isPresent();
    }

    @Test
    void aTripIdIsUniqueWithinATenant() {
        trips.saveAndFlush(trip("t1", "tenantA"));
        assertThatThrownBy(() -> trips.saveAndFlush(trip("t1", "tenantA")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
