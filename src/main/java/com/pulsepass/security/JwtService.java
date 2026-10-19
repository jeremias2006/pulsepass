package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String EXPECTED_ALGORITHM = "HS256";
    private static final int MIN_KEY_BYTES = 32;
    // Tolerancia de desfase de reloj al validar "iat" (token emitido "en el futuro")
    private static final Duration MAX_IAT_CLOCK_SKEW = Duration.ofSeconds(60);

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.secretBase64());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("PULSEPASS_JWT_SECRET_BASE64 is not valid Base64");
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "PULSEPASS_JWT_SECRET_BASE64 must decode to at least " + MIN_KEY_BYTES + " bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
        this.clock = clock;
    }

    public String generateAccessToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long getExpiresInSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /**
     * Valida firma, algoritmo, issuer, expiración y claims obligatorios.
     * @return el id del usuario (claim "sub")
     * @throws InvalidTokenException ante CUALQUIER problema con el token
     */
    public Long extractUserId(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))   // io.jsonwebtoken.Clock
                    .build()
                    .parseSignedClaims(token);                 // rechaza unsigned / alg=none

            if (!EXPECTED_ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
                throw new InvalidTokenException("Unexpected signing algorithm");
            }

            Claims claims = jws.getPayload();
            if (claims.getExpiration() == null
                    || claims.getIssuedAt() == null
                    || claims.getId() == null
                    || claims.getSubject() == null
                    || claims.getSubject().isBlank()) {
                throw new InvalidTokenException("Missing required claims");
            }
            validateIssuedAt(claims);
            return Long.valueOf(claims.getSubject());

        } catch (JwtException | IllegalArgumentException ex) {
            // JwtException: expirado, firma, malformado, unsupported...
            // IllegalArgumentException: token vacío o "sub" no numérico
            throw new InvalidTokenException("Invalid token", ex);
        }
    }

    /** PRD §12.1: rechazar "iat" inaceptable (en el futuro, o no anterior a "exp"). */
    private void validateIssuedAt(Claims claims) {
        Instant issuedAt = claims.getIssuedAt().toInstant();
        if (issuedAt.isAfter(clock.instant().plus(MAX_IAT_CLOCK_SKEW))) {
            throw new InvalidTokenException("Token issued in the future");
        }
        if (!claims.getExpiration().toInstant().isAfter(issuedAt)) {
            throw new InvalidTokenException("Token expiration must be after issuance");
        }
    }
}