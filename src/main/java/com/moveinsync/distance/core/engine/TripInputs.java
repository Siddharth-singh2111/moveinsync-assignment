package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Milestone;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.Rider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * All evidence for one trip, as handed to the engine. Collections are defensively copied and
 * null-tolerant; {@code plannedOffice}, {@code actualTripEnd} and {@code tripEndTime} may be
 * null when there is no office anchor to add.
 */
public record TripInputs(String tripId,
                         List<Ping> pings,
                         List<Milestone> milestones,
                         Map<String, Rider> ridersById,
                         GeoPoint plannedOffice,
                         GeoPoint actualTripEnd,
                         Instant tripEndTime) {

    public TripInputs {
        Objects.requireNonNull(tripId, "tripId");
        pings = pings == null ? List.of() : List.copyOf(pings);
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
        ridersById = ridersById == null ? Map.of() : Map.copyOf(ridersById);
    }
}
