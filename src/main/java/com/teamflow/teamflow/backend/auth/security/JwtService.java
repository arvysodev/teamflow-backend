package com.teamflow.teamflow.backend.auth.security;

import com.teamflow.teamflow.backend.users.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;
    private final String issuer;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-ttl-seconds}") long ttlSeconds,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
        this.issuer = issuer;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
    }

    public String extractUserId(String token) {
        return parseAndValidate(token).getPayload().getSubject();
    }

    public String extractEmail(String token) {
        return parseAndValidate(token).getPayload().get("email", String.class);
    }

    public String extractRole(String token) {
        return parseAndValidate(token).getPayload().get("role", String.class);
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
