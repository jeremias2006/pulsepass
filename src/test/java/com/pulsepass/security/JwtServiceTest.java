package com.pulsepass.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMhISE=";
    private static final String ISSUER = "pulsepass-test";
    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");

    private JwtService service(Instant at, String issuer, String secret) {
        return new JwtService(
                new JwtProperties(issuer, secret, Duration.ofMinutes(15)),
                Clock.fixed(at, ZoneOffset.UTC));
    }

    @Test
    void generatesAndValidatesToken() {
        JwtService service = service(NOW, ISSUER, SECRET);

        String token = service.generateAccessToken(42L);

        assertThat(service.extractUserId(token)).isEqualTo(42L);
        assertThat(service.getExpiresInSeconds()).isEqualTo(900);
    }

    @Test
    void rejectsExpiredToken() {
        String token = service(NOW, ISSUER, SECRET).generateAccessToken(42L);
        JwtService sixteenMinutesLater = service(NOW.plusSeconds(16 * 60), ISSUER, SECRET);

        assertThatThrownBy(() -> sixteenMinutesLater.extractUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsWrongIssuer() {
        String token = service(NOW, "otro-emisor", SECRET).generateAccessToken(42L);

        assertThatThrownBy(() -> service(NOW, ISSUER, SECRET).extractUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsTamperedSignature() {
        JwtService service = service(NOW, ISSUER, SECRET);
        String[] parts = service.generateAccessToken(42L).split("\\.");
        char first = parts[2].charAt(0);
        String tampered = parts[0] + "." + parts[1] + "."
                + (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        assertThatThrownBy(() -> service.extractUserId(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsUnsignedToken() {
        String unsigned = Jwts.builder().subject("42").issuer(ISSUER).compact();   // alg=none

        assertThatThrownBy(() -> service(NOW, ISSUER, SECRET).extractUserId(unsigned))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String otherSecret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        String token = service(NOW, ISSUER, otherSecret).generateAccessToken(42L);

        assertThatThrownBy(() -> service(NOW, ISSUER, SECRET).extractUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsEmptyAndMalformedTokens() {
        JwtService service = service(NOW, ISSUER, SECRET);

        assertThatThrownBy(() -> service.extractUserId("")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service.extractUserId("abc")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service.extractUserId("a.b.c")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void failsFastWithWeakKey() {
        String weak = Encoders.BASE64.encode(new byte[16]);   // 16 bytes < 32

        assertThatThrownBy(() -> service(NOW, ISSUER, weak))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- claim "iat" (PRD §12.1) ----------

    /** Token firmado correctamente pero con iat/exp controlados por el test. iat == null => sin claim. */
    private String signedToken(Instant issuedAt, Instant expiration) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        var builder = Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString());
        if (issuedAt != null) {
            builder.issuedAt(Date.from(issuedAt));
        }
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    @Test
    void rejectsTokenWithoutIssuedAt() {
        String token = signedToken(null, NOW.plusSeconds(900));

        assertThatThrownBy(() -> service(NOW, ISSUER, SECRET).extractUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsTokenIssuedFarInTheFuture() {
        String token = signedToken(NOW.plusSeconds(600), NOW.plusSeconds(1500));

        assertThatThrownBy(() -> service(NOW, ISSUER, SECRET).extractUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void acceptsIssuedAtWithinClockSkew() {
        String token = signedToken(NOW.plusSeconds(30), NOW.plusSeconds(930));

        assertThat(service(NOW, ISSUER, SECRET).extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void rejectsExpirationNotAfterIssuedAt() {
        // iat dentro de la tolerancia, exp futura pero anterior al iat
        String token = signedToken(NOW.plusSeconds(30), NOW.plusSeconds(10));

        assertThatThrownBy(() -> service(NOW, ISSUER, SECRET).extractUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }
}