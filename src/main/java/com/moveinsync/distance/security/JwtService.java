package com.moveinsync.distance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies HMAC-signed JWTs carrying the tenant and role. The tenant on every request
 * comes from here, so a caller cannot act outside the tenant their token was issued for.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMinutes;

    public JwtService(AuthProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = props.tokenTtlMinutes();
    }

    public String issue(String subject, String tenantId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("tenantId", tenantId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(ttlMinutes))))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return new AuthenticatedUser(
                claims.getSubject(),
                claims.get("tenantId", String.class),
                Role.valueOf(claims.get("role", String.class)));
    }
}
