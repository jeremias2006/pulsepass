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

/**
 * Crea y valida los tokens JWT de acceso (librería JJWT).
 *
 * <p>Un JWT son tres partes en Base64: cabecera (algoritmo), cuerpo con los "claims" (datos) y
 * firma. La firma se calcula con una clave secreta que solo conoce el servidor (HMAC-SHA256);
 * si alguien altera el contenido, la firma deja de coincidir y el token se rechaza.</p>
 *
 * <p>Los tokens de este sistema llevan: {@code sub} (id del usuario), {@code iss} (emisor),
 * {@code iat} (cuándo se emitió), {@code exp} (cuándo vence) y {@code jti} (id único del token).
 * NO llevan roles: los roles se consultan a la base en cada petición (ver CustomUserDetailsService).</p>
 */
@Service
public class JwtService {

    // Único algoritmo aceptado: HMAC con SHA-256 (clave simétrica: la misma clave firma y verifica).
    private static final String EXPECTED_ALGORITHM = "HS256";
    // 32 bytes = 256 bits: tamaño mínimo recomendado para una clave HS256.
    private static final int MIN_KEY_BYTES = 32;
    // Tolerancia de desfase de reloj al validar "iat" (token emitido "en el futuro")
    private static final Duration MAX_IAT_CLOCK_SKEW = Duration.ofSeconds(60);

    private final SecretKey key;            // clave de firma/verificación (HMAC)
    private final String issuer;            // valor esperado del claim "iss"
    private final Duration accessTokenTtl;  // cuánto dura un token desde que se emite
    private final Clock clock;              // reloj inyectable (permite fijar la hora en pruebas)

    /**
     * Decodifica y valida la clave al construir el bean. Si la clave no es Base64 válido o es
     * demasiado corta, la aplicación NO arranca (falla rápido en vez de operar con una clave débil).
     * Los mensajes de error nombran la variable de entorno pero nunca muestran el valor del secreto.
     */
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
        // Construye la clave HMAC a partir de los bytes decodificados.
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
        this.clock = clock;
    }

    /**
     * Emite un token de acceso para el usuario indicado.
     *
     * @param userId id del usuario (queda en el claim "sub")
     * @return el JWT firmado, listo para enviarse como {@code Authorization: Bearer <token>}
     */
    public String generateAccessToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))                 // sub: a quién pertenece el token
                .issuer(issuer)                                  // iss: quién lo emitió
                .issuedAt(Date.from(now))                        // iat: momento de emisión
                .expiration(Date.from(now.plus(accessTokenTtl))) // exp: emisión + vigencia (15 min por configuración)
                .id(UUID.randomUUID().toString())                // jti: identificador único de este token
                .signWith(key, Jwts.SIG.HS256)                   // firma con la clave secreta
                .compact();
    }

    /** Vigencia del token en segundos; se devuelve al cliente en AuthResponse junto al token. */
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
            // El parser verifica la firma con nuestra clave, exige el emisor correcto y comprueba que
            // el token no haya vencido (usando nuestro reloj inyectable).
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))   // io.jsonwebtoken.Clock
                    .build()
                    .parseSignedClaims(token);                 // rechaza unsigned / alg=none

            // Defensa extra: aunque la firma sea válida, el algoritmo declarado en la cabecera debe ser HS256.
            if (!EXPECTED_ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
                throw new InvalidTokenException("Unexpected signing algorithm");
            }

            // Los claims exp, iat, jti y sub son obligatorios: un token firmado pero incompleto se rechaza.
            Claims claims = jws.getPayload();
            if (claims.getExpiration() == null
                    || claims.getIssuedAt() == null
                    || claims.getId() == null
                    || claims.getSubject() == null
                    || claims.getSubject().isBlank()) {
                throw new InvalidTokenException("Missing required claims");
            }
            validateIssuedAt(claims);
            // El claim "sub" es el id del usuario; si no es un número, Long.valueOf lanza una excepción
            // que el catch de abajo convierte en InvalidTokenException.
            return Long.valueOf(claims.getSubject());

        } catch (JwtException | IllegalArgumentException ex) {
            // JwtException: expirado, firma, malformado, unsupported...
            // IllegalArgumentException: token vacío o "sub" no numérico
            throw new InvalidTokenException("Invalid token", ex);
        }
    }

    /**
     * PRD §12.1: rechazar "iat" inaceptable (en el futuro, o no anterior a "exp").
     * Cubre tokens manipulados o emitidos por un reloj desajustado.
     */
    private void validateIssuedAt(Claims claims) {
        Instant issuedAt = claims.getIssuedAt().toInstant();
        // Se tolera hasta 60 s de diferencia entre relojes; más que eso es un token "emitido en el futuro".
        if (issuedAt.isAfter(clock.instant().plus(MAX_IAT_CLOCK_SKEW))) {
            throw new InvalidTokenException("Token issued in the future");
        }
        // Coherencia: el vencimiento debe ser posterior a la emisión.
        if (!claims.getExpiration().toInstant().isAfter(issuedAt)) {
            throw new InvalidTokenException("Token expiration must be after issuance");
        }
    }
}