package com.moveinsync.distance.core.model;

/**
 * Where an {@link Anchor}'s location came from. Carried through to the output so a vendor
 * dispute can see not just the distance but the evidence each anchor rests on.
 */
public enum AnchorSource {
    ACTUAL_PICKUP,
    ACTUAL_DROP,
    PROFILE_HOME,
    PLANNED,
    NO_SHOW,
    ACTUAL_OFFICE,
    PLANNED_OFFICE
}
