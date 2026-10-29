package com.moveinsync.distance.core.anchor;

import com.moveinsync.distance.core.distance.Haversine;
import com.moveinsync.distance.core.model.AnchorSource;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Milestone;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.Rider;
import com.moveinsync.distance.core.model.TimelinePoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the trusted trip timeline: the cleaned pings plus anchor points that patch the gaps,
 * all merged in time order.
 *
 * <p>Employee anchors follow a fixed fallback chain — actual pickup/drop if valid, then the
 * ad-hoc rider's profile home and sign-in time, then the planned location and planned time.
 * No-show anchors are added only when enabled. The office anchor uses the actual trip-end point
 * when it is within the office radius, otherwise the planned office at the trip-end time.
 */
public final class AnchorBuilder {

    private final boolean includeNoShowAnchors;
    private final double officeRadiusMeters;

    public AnchorBuilder(boolean includeNoShowAnchors, double officeRadiusMeters) {
        if (officeRadiusMeters < 0) {
            throw new IllegalArgumentException("officeRadiusMeters must be >= 0");
        }
        this.includeNoShowAnchors = includeNoShowAnchors;
        this.officeRadiusMeters = officeRadiusMeters;
    }

    /**
     * Merge cleaned pings and anchors into one time-ordered timeline.
     *
     * @param cleanedPings  chronological, cleaned pings
     * @param milestones    trip milestones (pickups, drops, no-shows)
     * @param ridersById    rider fallback evidence, keyed by rider id
     * @param plannedOffice planned office location (may be null to skip the office anchor)
     * @param actualTripEnd actual trip-end point (may be null)
     * @param tripEndTime   trip-end time (may be null to skip the office anchor)
     */
    public List<TimelinePoint> build(List<Ping> cleanedPings,
                                     List<Milestone> milestones,
                                     Map<String, Rider> ridersById,
                                     GeoPoint plannedOffice,
                                     GeoPoint actualTripEnd,
                                     Instant tripEndTime) {
        Objects.requireNonNull(cleanedPings, "cleanedPings");
        Objects.requireNonNull(milestones, "milestones");
        Objects.requireNonNull(ridersById, "ridersById");

        List<TimelinePoint> points = new ArrayList<>();

        for (Ping p : cleanedPings) {
            points.add(new TimelinePoint(p.location(), p.timestamp(), null));
        }

        for (Milestone m : milestones) {
            switch (m.kind()) {
                case PICKUP -> addEmployeeAnchor(points, m, ridersById, AnchorSource.ACTUAL_PICKUP);
                case DROP -> addEmployeeAnchor(points, m, ridersById, AnchorSource.ACTUAL_DROP);
                case NO_SHOW -> {
                    if (includeNoShowAnchors) {
                        addNoShowAnchor(points, m, ridersById);
                    }
                }
                case TRIP_START -> {
                    // The start is already represented by the first pings; no anchor needed.
                }
            }
        }

        addOfficeAnchor(points, plannedOffice, actualTripEnd, tripEndTime);

        points.sort(Comparator.comparing(TimelinePoint::time));
        return List.copyOf(points);
    }

    private void addEmployeeAnchor(List<TimelinePoint> points,
                                   Milestone m,
                                   Map<String, Rider> ridersById,
                                   AnchorSource actualSource) {
        if (m.location() != null && m.time() != null) {
            points.add(new TimelinePoint(m.location(), m.time(), actualSource));
            return;
        }
        Rider rider = m.riderId() == null ? null : ridersById.get(m.riderId());
        if (rider == null) {
            return;
        }
        if (rider.home() != null && rider.signInTime() != null) {
            points.add(new TimelinePoint(rider.home(), rider.signInTime(), AnchorSource.PROFILE_HOME));
            return;
        }
        if (rider.plannedLocation() != null && rider.plannedTime() != null) {
            points.add(new TimelinePoint(rider.plannedLocation(), rider.plannedTime(), AnchorSource.PLANNED));
        }
    }

    private void addNoShowAnchor(List<TimelinePoint> points,
                                 Milestone m,
                                 Map<String, Rider> ridersById) {
        if (m.location() != null && m.time() != null) {
            points.add(new TimelinePoint(m.location(), m.time(), AnchorSource.NO_SHOW));
            return;
        }
        Rider rider = m.riderId() == null ? null : ridersById.get(m.riderId());
        if (rider != null && rider.plannedLocation() != null && rider.plannedTime() != null) {
            points.add(new TimelinePoint(rider.plannedLocation(), rider.plannedTime(), AnchorSource.NO_SHOW));
        }
    }

    private void addOfficeAnchor(List<TimelinePoint> points,
                                 GeoPoint plannedOffice,
                                 GeoPoint actualTripEnd,
                                 Instant tripEndTime) {
        if (plannedOffice == null || tripEndTime == null) {
            return;
        }
        if (actualTripEnd != null
                && Haversine.meters(actualTripEnd, plannedOffice) <= officeRadiusMeters) {
            points.add(new TimelinePoint(actualTripEnd, tripEndTime, AnchorSource.ACTUAL_OFFICE));
        } else {
            points.add(new TimelinePoint(plannedOffice, tripEndTime, AnchorSource.PLANNED_OFFICE));
        }
    }
}
