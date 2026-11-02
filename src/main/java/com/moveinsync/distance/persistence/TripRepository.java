package com.moveinsync.distance.persistence;

import com.moveinsync.distance.core.model.ResultStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Trip storage. Every lookup is scoped by tenant, so one client can never read another's trips.
 */
public interface TripRepository extends JpaRepository<TripEntity, Long> {

    Optional<TripEntity> findByTripIdAndTenantId(String tripId, String tenantId);

    /** Loads a trip for a change, holding its row so concurrent writes to one trip run in turn. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TripEntity t where t.tripId = :tripId and t.tenantId = :tenantId")
    Optional<TripEntity> findForUpdate(@Param("tripId") String tripId, @Param("tenantId") String tenantId);

    /** Trips whose most recent compute had this status, across all tenants, billed or not. */
    List<TripEntity> findByLatestResultStatus(ResultStatus latestResultStatus);
}
