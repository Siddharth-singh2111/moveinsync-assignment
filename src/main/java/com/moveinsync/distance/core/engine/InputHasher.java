package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Milestone;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.Rider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Produces a stable SHA-256 fingerprint of a trip's inputs plus the rule version. The same
 * inputs under the same version always hash the same, which is what lets a recompute decide
 * whether anything actually changed before touching a billed trip.
 */
final class InputHasher {

    private InputHasher() {
    }

    static String hash(TripInputs in, int ruleVersion) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("v").append(ruleVersion).append('|').append(in.tripId()).append('|');

        for (Ping p : in.pings()) {
            sb.append('P')
                    .append(p.location().latitude()).append(',')
                    .append(p.location().longitude()).append(',')
                    .append(p.timestamp().toEpochMilli()).append(',')
                    .append(p.accuracyMeters()).append(';');
        }
        sb.append('|');
        for (Milestone m : in.milestones()) {
            sb.append('M')
                    .append(m.kind()).append(',')
                    .append(m.riderId()).append(',')
                    .append(point(m.location())).append(',')
                    .append(m.time() == null ? "" : m.time().toEpochMilli()).append(';');
        }
        sb.append('|');
        in.ridersById().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    Rider r = e.getValue();
                    sb.append('R').append(e.getKey()).append(',').append(r.kind()).append(',')
                            .append(point(r.home())).append(',')
                            .append(r.signInTime() == null ? "" : r.signInTime().toEpochMilli()).append(',')
                            .append(point(r.plannedLocation())).append(',')
                            .append(r.plannedTime() == null ? "" : r.plannedTime().toEpochMilli()).append(';');
                });
        sb.append('|').append(point(in.plannedOffice()))
                .append('|').append(point(in.actualTripEnd()))
                .append('|').append(in.tripEndTime() == null ? "" : in.tripEndTime().toEpochMilli());

        return sha256Hex(sb.toString());
    }

    private static String point(GeoPoint p) {
        return p == null ? "" : p.latitude() + "," + p.longitude();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
