package com.moveinsync.distance.persistence;

import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Trip storage. Every lookup is scoped by tenant, so one client can never read another's trips.
 */
public interface TripRepository extends JpaRepository<TripEntity, String> {

    Optional<TripEntity> findByTripIdAndTenantId(String tripId, String tenantId);

    /** Degraded trips that are not yet billed — candidates for automatic recovery. */
    List<TripEntity> findByStatusNotAndResultStatus(TripStatus status, ResultStatus resultStatus);
}
