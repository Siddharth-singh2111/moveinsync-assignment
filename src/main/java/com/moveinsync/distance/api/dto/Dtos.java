package com.moveinsync.distance.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Request and response payloads for the API, grouped as nested records. Kept separate from the
 * core model so the wire format can evolve independently of the engine.
 */
public final class Dtos {

    private Dtos() {
    }

    public record PingDto(double lat, double lon, Instant timestamp, double accuracyMeters) {
    }

    public record MilestoneDto(String kind, String riderId, Double lat, Double lon, Instant time) {
    }

    public record RiderDto(String riderId, String kind,
                           Double homeLat, Double homeLon, Instant signInTime,
                           Double plannedLat, Double plannedLon, Instant plannedTime) {
    }

    public record ComputeTripRequest(@NotBlank String tripId,
                                     List<PingDto> pings,
                                     List<MilestoneDto> milestones,
                                     List<RiderDto> riders,
                                     Double officeLat, Double officeLon,
                                     Double tripEndLat, Double tripEndLon,
                                     Instant tripEndTime) {
    }

    public record IngestPingsRequest(List<PingDto> pings) {
    }

    public record SegmentDto(double fromLat, double fromLon, double toLat, double toLon,
                             long meters, String rule, boolean providerCall) {
    }

    public record DeltaDto(BigDecimal deltaKm, long deltaMeters, String reason,
                           String fromRef, String toRef, Instant createdAt) {
    }

    public record DistanceResponse(String tripId, BigDecimal totalKm, long totalMeters, String status,
                                   int providerCalls, int ruleVersion, String inputHash,
                                   Instant computedAt, DeltaDto appendedDelta) {
    }

    public record ExplainResponse(String tripId, BigDecimal totalKm, String status, List<SegmentDto> segments) {
    }

    public record DeltasResponse(String tripId, List<DeltaDto> deltas) {
    }

    public record ErrorResponse(String error, String message) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, String tenantId, String role) {
    }
}
