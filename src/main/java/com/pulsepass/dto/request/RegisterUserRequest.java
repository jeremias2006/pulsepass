package com.pulsepass.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;   // ver nota
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

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
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
        String password
) {

        // BCrypt solo procesa 72 BYTES; @Size cuenta caracteres (un "ñ" son 2 bytes)
        @JsonIgnore
        @AssertTrue(message = "Password must not exceed 72 bytes")
        public boolean isPasswordWithinBcryptLimit() {
                return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }

        @Override
        public String toString() {
                return "RegisterUserRequest[username=" + username + ", email=" + email + ", password=***]";
        }
}