package com.moveinsync.distance.persistence;

import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.TripStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * A trip and its latest computed result. The raw inputs and the segment breakdown are stored as
 * JSON so a recompute can replay them; the totals and hash are columns so they can be queried and
 * compared cheaply. A trip id is unique only within its tenant, so two clients can use the same id
 * without touching each other's trip; {@code tenantId} scopes every access.
 */
@Entity
@Table(name = "trips", uniqueConstraints = @UniqueConstraint(
        name = "uk_trips_tenant_trip", columnNames = {"tenant_id", "trip_id"}))
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private String tripId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Column(name = "rule_version", nullable = false)
    private int ruleVersion;

    @Column(name = "input_hash", nullable = false)
    private String inputHash;

    @Column(name = "total_meters", nullable = false)
    private long totalMeters;

    @Column(name = "provider_calls", nullable = false)
    private int providerCalls;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false)
    private ResultStatus resultStatus;

    /**
     * How the most recent compute went. Differs from {@code resultStatus} on a billed trip, whose
     * result stays frozen: this is what tells the recovery job a billed trip still needs correcting.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "latest_result_status", nullable = false)
    private ResultStatus latestResultStatus;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "inputs_json", nullable = false)
    private String inputsJson;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "segments_json", nullable = false)
    private String segmentsJson;

    public TripEntity() {
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

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public int getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(int ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getInputHash() {
        return inputHash;
    }

    public void setInputHash(String inputHash) {
        this.inputHash = inputHash;
    }

    public long getTotalMeters() {
        return totalMeters;
    }

    public void setTotalMeters(long totalMeters) {
        this.totalMeters = totalMeters;
    }

    public int getProviderCalls() {
        return providerCalls;
    }

    public void setProviderCalls(int providerCalls) {
        this.providerCalls = providerCalls;
    }

    public ResultStatus getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(ResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    public ResultStatus getLatestResultStatus() {
        return latestResultStatus;
    }

    public void setLatestResultStatus(ResultStatus latestResultStatus) {
        this.latestResultStatus = latestResultStatus;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public String getInputsJson() {
        return inputsJson;
    }

    public void setInputsJson(String inputsJson) {
        this.inputsJson = inputsJson;
    }

    public String getSegmentsJson() {
        return segmentsJson;
    }

    public void setSegmentsJson(String segmentsJson) {
        this.segmentsJson = segmentsJson;
    }
}
