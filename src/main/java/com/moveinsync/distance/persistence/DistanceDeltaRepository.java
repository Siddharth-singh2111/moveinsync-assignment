package com.moveinsync.distance.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * The append-only delta ledger, read back per trip in chronological order. Tenant-scoped.
 */
public interface DistanceDeltaRepository extends JpaRepository<DistanceDeltaEntity, Long> {

    List<DistanceDeltaEntity> findByTripIdAndTenantIdOrderByCreatedAtAscIdAsc(String tripId, String tenantId);
}
