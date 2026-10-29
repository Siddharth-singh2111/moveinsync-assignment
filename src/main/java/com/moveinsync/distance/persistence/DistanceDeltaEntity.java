package com.moveinsync.distance.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One row in the append-only signed-delta ledger. Rows are only ever inserted — a billed trip's
 * distance is corrected by adding deltas, never by rewriting it.
 */
@Entity
@Table(name = "distance_deltas")
public class DistanceDeltaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private String tripId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "from_ref", nullable = false)
    private String fromRef;

    @Column(name = "to_ref", nullable = false)
    private String toRef;

    @Column(name = "delta_meters", nullable = false)
    private long deltaMeters;

    @Column(nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DistanceDeltaEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getFromRef() {
        return fromRef;
    }

    public void setFromRef(String fromRef) {
        this.fromRef = fromRef;
    }

    public String getToRef() {
        return toRef;
    }

    public void setToRef(String toRef) {
        this.toRef = toRef;
    }

    public long getDeltaMeters() {
        return deltaMeters;
    }

    public void setDeltaMeters(long deltaMeters) {
        this.deltaMeters = deltaMeters;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
