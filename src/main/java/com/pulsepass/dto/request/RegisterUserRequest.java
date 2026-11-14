package com.pulsepass.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;   // ver nota
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * DTO de ENTRADA: representa el JSON que el cliente envía. Es un {@code record} (inmutable, con constructor y accesores generados). Las anotaciones de Bean Validation ({@code @NotBlank}: no nulo ni vacío; {@code @NotNull}: no nulo; {@code @Size}: longitud; {@code @Email}: formato) se ejecutan cuando el controlador lo recibe con {@code @Valid}; si alguna falla, la respuesta es 400 con el detalle por campo en {@code details}. Son validaciones de FORMA: las reglas de negocio las valida el servicio.
 *
 * <p>Datos de registro de un usuario: cuenta (username, email, contraseña) y perfil
 * (nombre, apellido, teléfono, ciudad, fecha de nacimiento). {@code toString} oculta la contraseña.</p>
 */
public record RegisterUserRequest(

        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate,

        @NotBlank(message = "Password is required")
        // Mínimo 12 caracteres; el máximo de 72 existe porque BCrypt ignora lo que exceda de 72 bytes.
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
        String password
) {

        // BCrypt solo procesa 72 BYTES; @Size cuenta caracteres (un "ñ" son 2 bytes)
        // @AssertTrue sobre un método isXxx() hace que Bean Validation lo evalúe como si fuera un campo más:
        // si devuelve false, la petición falla con 400. @JsonIgnore evita que Jackson lo trate como una
        // propiedad del JSON (no se lee ni se escribe).
        @JsonIgnore
        @AssertTrue(message = "Password must not exceed 72 bytes")
        public boolean isPasswordWithinBcryptLimit() {
                return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }

        // Sobrescribe toString para enmascarar la contraseña si alguien imprime o registra el objeto.
        @Override
        public String toString() {
                return "RegisterUserRequest[username=" + username + ", email=" + email + ", password=***]";
        }
}