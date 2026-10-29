package com.moveinsync.distance.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Authentication configuration: the JWT signing secret, token lifetime, and the demo user list.
 * In production the users would come from a real identity provider, not config.
 */
@ConfigurationProperties(prefix = "distance.auth")
public record AuthProperties(String secret, long tokenTtlMinutes, List<User> users) {

    public record User(String username, String password, String tenantId, String role) {
    }
}
