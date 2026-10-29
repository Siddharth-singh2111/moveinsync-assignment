package com.moveinsync.distance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moveinsync.distance.core.engine.Deltas;
import com.moveinsync.distance.core.engine.DistanceEngine;
import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.DistanceDelta;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.TripStatus;
import com.moveinsync.distance.metrics.DistanceMetrics;
import com.moveinsync.distance.persistence.DistanceDeltaEntity;
import com.moveinsync.distance.persistence.DistanceDeltaRepository;
import com.moveinsync.distance.persistence.JsonCodec;
import com.moveinsync.distance.persistence.TripEntity;
import com.moveinsync.distance.persistence.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application service over the engine and the stores. Enforces tenant scoping on every access,
 * keeps a billed trip's result immutable (recording corrections as deltas), and overwrites the
 * stored result only while the trip is not yet billed.
 *
 * <p>Logs carry only non-identifying fields (trip id, tenant, totals) — never coordinates or any
 * personal data.
 */
@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);
    private static final TypeReference<List<Segment>> SEGMENT_LIST = new TypeReference<>() {
    };

    private final DistanceEngine engine;
    private final TripRepository trips;
    private final DistanceDeltaRepository deltas;
    private final JsonCodec json;
    private final DistanceMetrics metrics;
    private final Clock clock;

    public TripService(DistanceEngine engine, TripRepository trips, DistanceDeltaRepository deltas,
                       JsonCodec json, DistanceMetrics metrics, Clock clock) {
        this.engine = engine;
        this.trips = trips;
        this.deltas = deltas;
        this.json = json;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public TripComputation compute(String tenantId, TripInputs inputs) {
        return persist(tenantId, inputs, run(inputs), "compute");
    }

    @Transactional
    public TripComputation recompute(String tenantId, String tripId, String reason) {
        TripInputs inputs = json.read(require(tenantId, tripId).getInputsJson(), TripInputs.class);
        return persist(tenantId, inputs, run(inputs), reason);
    }

    @Transactional
    public TripComputation ingestPings(String tenantId, String tripId, List<Ping> newPings) {
        TripInputs existing = json.read(require(tenantId, tripId).getInputsJson(), TripInputs.class);
        List<Ping> merged = new ArrayList<>(existing.pings());
        merged.addAll(newPings);
        TripInputs updated = new TripInputs(existing.tripId(), merged, existing.milestones(),
                existing.ridersById(), existing.plannedOffice(), existing.actualTripEnd(), existing.tripEndTime());
        return persist(tenantId, updated, run(updated), "late-pings");
    }

    @Transactional
    public void bill(String tenantId, String tripId) {
        TripEntity e = require(tenantId, tripId);
        e.setStatus(TripStatus.BILLED);
        trips.save(e);
        log.info("trip {} tenant {} billed", tripId, tenantId);
    }

    @Transactional(readOnly = true)
    public DistanceResult result(String tenantId, String tripId) {
        return toResult(require(tenantId, tripId));
    }

    @Transactional(readOnly = true)
    public List<DistanceDelta> ledger(String tenantId, String tripId) {
        require(tenantId, tripId);
        return deltas.findByTripIdAndTenantIdOrderByCreatedAtAscIdAsc(tripId, tenantId)
                .stream().map(TripService::toDelta).toList();
    }

    private DistanceResult run(TripInputs inputs) {
        DistanceResult result = metrics.time(() -> engine.compute(inputs));
        metrics.recordComputation(result);
        return result;
    }

    private TripComputation persist(String tenantId, TripInputs inputs, DistanceResult result, String reason) {
        Optional<TripEntity> existing = trips.findByTripIdAndTenantId(inputs.tripId(), tenantId);

        if (existing.isPresent() && existing.get().getStatus() == TripStatus.BILLED) {
            // A billed trip's number is never rewritten; a change is recorded as a signed delta.
            DistanceResult previous = toResult(existing.get());
            DistanceDelta appended = Deltas.between(previous, result, reason, Instant.now(clock))
                    .map(delta -> {
                        deltas.save(toDeltaEntity(tenantId, delta));
                        metrics.recordDeltaAppended();
                        log.info("trip {} tenant {} delta {}m reason={}",
                                delta.tripId(), tenantId, delta.deltaMeters(), reason);
                        return delta;
                    })
                    .orElse(null);
            return new TripComputation(previous, appended, TripStatus.BILLED);
        }

        TripEntity entity = existing.orElseGet(TripEntity::new);
        boolean isNew = existing.isEmpty();
        fill(entity, tenantId, inputs, result);
        if (isNew) {
            entity.setStatus(TripStatus.COMPUTED);
        }
        trips.save(entity);
        log.info("trip {} tenant {} computed total={}m status={} providerCalls={}",
                result.tripId(), tenantId, result.totalMeters(), result.status(), result.providerCalls());
        return new TripComputation(result, null, entity.getStatus());
    }

    private TripEntity require(String tenantId, String tripId) {
        return trips.findByTripIdAndTenantId(tripId, tenantId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
    }

    private void fill(TripEntity e, String tenantId, TripInputs inputs, DistanceResult result) {
        e.setTripId(result.tripId());
        e.setTenantId(tenantId);
        e.setRuleVersion(result.ruleVersion());
        e.setInputHash(result.inputHash());
        e.setTotalMeters(result.totalMeters());
        e.setProviderCalls(result.providerCalls());
        e.setResultStatus(result.status());
        e.setComputedAt(result.computedAt());
        e.setInputsJson(json.write(inputs));
        e.setSegmentsJson(json.write(result.segments()));
    }

    private DistanceResult toResult(TripEntity e) {
        List<Segment> segments = json.read(e.getSegmentsJson(), SEGMENT_LIST);
        return new DistanceResult(e.getTripId(), e.getTotalMeters(), segments, e.getProviderCalls(),
                e.getResultStatus(), e.getRuleVersion(), e.getInputHash(), e.getComputedAt());
    }

    private DistanceDeltaEntity toDeltaEntity(String tenantId, DistanceDelta d) {
        DistanceDeltaEntity e = new DistanceDeltaEntity();
        e.setTripId(d.tripId());
        e.setTenantId(tenantId);
        e.setFromRef(d.fromRef());
        e.setToRef(d.toRef());
        e.setDeltaMeters(d.deltaMeters());
        e.setReason(d.reason());
        e.setCreatedAt(d.createdAt());
        return e;
    }

    private static DistanceDelta toDelta(DistanceDeltaEntity e) {
        return new DistanceDelta(e.getTripId(), e.getFromRef(), e.getToRef(),
                e.getDeltaMeters(), e.getReason(), e.getCreatedAt());
    }
}
