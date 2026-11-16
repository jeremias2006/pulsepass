package com.pulsepass.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de ENTRADA: representa el JSON que el cliente envía. Es un {@code record} (inmutable, con constructor y accesores generados). Las anotaciones de Bean Validation ({@code @NotBlank}: no nulo ni vacío; {@code @NotNull}: no nulo; {@code @Size}: longitud; {@code @Email}: formato) se ejecutan cuando el controlador lo recibe con {@code @Valid}; si alguna falla, la respuesta es 400 con el detalle por campo en {@code details}. Son validaciones de FORMA: las reglas de negocio las valida el servicio.
 *
 * <p>Credenciales de inicio de sesión. El {@code identifier} puede ser username o email.
 * {@code toString} oculta la contraseña para que nunca aparezca en los logs.</p>
 */
public record LoginRequest(

        @NotBlank(message = "Identifier is required")
        @Size(max = 150, message = "Identifier must not exceed 150 characters")
        String identifier,   // username o email

        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password must not exceed 128 characters")
        String password
) {

    // Sobrescribe toString para enmascarar la contraseña si alguien imprime o registra el objeto.
    @Override
    public String toString() {
        return "LoginRequest[identifier=" + identifier + ", password=***]";
    }
}