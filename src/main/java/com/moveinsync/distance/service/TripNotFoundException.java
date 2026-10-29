package com.moveinsync.distance.service;

/**
 * A trip does not exist for the caller's tenant. Deliberately does not distinguish "never
 * existed" from "belongs to another tenant" — both are a 404, so tenant membership never leaks.
 */
public class TripNotFoundException extends RuntimeException {

    public TripNotFoundException(String tripId) {
        super("trip not found: " + tripId);
    }
}
