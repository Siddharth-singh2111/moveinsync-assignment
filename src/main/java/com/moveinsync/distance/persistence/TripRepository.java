package com.moveinsync.distance.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Trip storage. Every lookup is scoped by tenant, so one client can never read another's trips.
 */
public interface TripRepository extends JpaRepository<TripEntity, String> {

    Optional<TripEntity> findByTripIdAndTenantId(String tripId, String tenantId);
}
