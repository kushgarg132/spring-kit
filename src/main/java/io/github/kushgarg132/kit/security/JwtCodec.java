package io.github.kushgarg132.kit.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;

/**
 * Issues and verifies HMAC-signed access tokens. The key is the secret's UTF-8 bytes, so tokens
 * issued by the apps' older hand-rolled providers with the same secret stay valid.
 */
public class JwtCodec {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration ttl;
    private final Clock clock;

    public JwtCodec(String secret, Duration ttl) {
        this(secret, ttl, Clock.systemUTC());
    }

    public JwtCodec(String secret, Duration ttl, Clock clock) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            // Fail at startup, not on the first login.
            throw new IllegalStateException("JWT secret must be at least " + MIN_SECRET_BYTES
                    + " bytes. Generate one with: openssl rand -base64 48");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttl = ttl;
        this.clock = clock;
    }

    public Duration ttl() {
        return ttl;
    }

    public String issue(String subject, Map<String, ?> claims) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Empty when the token is malformed, expired, or signed with another key. */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
