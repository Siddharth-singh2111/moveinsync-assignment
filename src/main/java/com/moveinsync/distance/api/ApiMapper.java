package com.moveinsync.distance.api;

import com.moveinsync.distance.api.dto.Dtos;
import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.DistanceDelta;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Milestone;
import com.moveinsync.distance.core.model.MilestoneKind;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.Rider;
import com.moveinsync.distance.core.model.RiderKind;
import com.moveinsync.distance.core.model.Segment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts between API DTOs and the core model. Pure, static, no framework. */
final class ApiMapper {

    private ApiMapper() {
    }

    static TripInputs toInputs(Dtos.ComputeTripRequest req) {
        List<Ping> pings = new ArrayList<>();
        if (req.pings() != null) {
            for (Dtos.PingDto p : req.pings()) {
                pings.add(new Ping(new GeoPoint(p.lat(), p.lon()), p.timestamp(), p.accuracyMeters()));
            }
        }
        List<Milestone> milestones = new ArrayList<>();
        if (req.milestones() != null) {
            for (Dtos.MilestoneDto m : req.milestones()) {
                milestones.add(new Milestone(MilestoneKind.valueOf(m.kind()), m.riderId(),
                        point(m.lat(), m.lon()), m.time()));
            }
        }
        Map<String, Rider> riders = new LinkedHashMap<>();
        if (req.riders() != null) {
            for (Dtos.RiderDto r : req.riders()) {
                riders.put(r.riderId(), new Rider(r.riderId(), RiderKind.valueOf(r.kind()),
                        point(r.homeLat(), r.homeLon()), r.signInTime(),
                        point(r.plannedLat(), r.plannedLon()), r.plannedTime()));
            }
        }
        return new TripInputs(req.tripId(), pings, milestones, riders,
                point(req.officeLat(), req.officeLon()), point(req.tripEndLat(), req.tripEndLon()),
                req.tripEndTime());
    }

    static List<Ping> toPings(Dtos.IngestPingsRequest req) {
        List<Ping> pings = new ArrayList<>();
        if (req.pings() != null) {
            for (Dtos.PingDto p : req.pings()) {
                pings.add(new Ping(new GeoPoint(p.lat(), p.lon()), p.timestamp(), p.accuracyMeters()));
            }
        }
        return pings;
    }

    static Dtos.DistanceResponse toDistanceResponse(DistanceResult r, DistanceDelta appendedDelta) {
        return new Dtos.DistanceResponse(r.tripId(), r.totalKm(), r.totalMeters(), r.status().name(),
                r.providerCalls(), r.ruleVersion(), r.inputHash(), r.computedAt(),
                appendedDelta == null ? null : toDeltaDto(appendedDelta));
    }

    static Dtos.ExplainResponse toExplainResponse(DistanceResult r) {
        List<Dtos.SegmentDto> segments = new ArrayList<>(r.segments().size());
        for (Segment s : r.segments()) {
            segments.add(new Dtos.SegmentDto(s.from().latitude(), s.from().longitude(),
                    s.to().latitude(), s.to().longitude(), s.meters(), s.rule().name(), s.providerCall()));
        }
        return new Dtos.ExplainResponse(r.tripId(), r.totalKm(), r.status().name(), segments);
    }

    static Dtos.DeltasResponse toDeltasResponse(String tripId, List<DistanceDelta> deltas) {
        return new Dtos.DeltasResponse(tripId, deltas.stream().map(ApiMapper::toDeltaDto).toList());
    }

    private static Dtos.DeltaDto toDeltaDto(DistanceDelta d) {
        return new Dtos.DeltaDto(d.deltaKm(), d.deltaMeters(), d.reason(), d.fromRef(), d.toRef(), d.createdAt());
    }

    private static GeoPoint point(Double lat, Double lon) {
        return (lat == null || lon == null) ? null : new GeoPoint(lat, lon);
    }
}
