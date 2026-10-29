package com.moveinsync.distance.security;

/** The caller resolved from a validated JWT: who they are, their tenant, and their role. */
public record AuthenticatedUser(String userId, String tenantId, Role role) {
}
