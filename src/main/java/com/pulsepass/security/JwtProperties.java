package com.pulsepass.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuración del JWT, leída de {@code application.properties} (prefijo
 * {@code pulsepass.security.jwt}). Es un {@code record}: inmutable y sin getters/setters.
 *
 * <p>Spring enlaza cada propiedad con el componente del record de mismo nombre
 * ({@code secret-base64} -> {@code secretBase64}). {@code @Validated} hace que la aplicación
 * falle al arrancar si falta algún valor obligatorio, en vez de fallar luego en medio de un login.</p>
 *
 * <ul>
 *   <li>{@code issuer}: quién emite el token (variable PULSEPASS_JWT_ISSUER; por defecto "pulsepass-api").</li>
 *   <li>{@code secretBase64}: clave secreta de firma en Base64 (variable PULSEPASS_JWT_SECRET_BASE64, obligatoria).</li>
 *   <li>{@code accessTokenTtl}: vigencia del token; en el properties es {@code PT15M} (formato ISO-8601 = 15 minutos).</li>
 * </ul>
 */
@Validated
@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String secretBase64,
        @NotNull Duration accessTokenTtl
) {
}