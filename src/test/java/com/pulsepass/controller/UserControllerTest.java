package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final String VALID_BODY = """
            {
              "username": "andrea",
              "email": "andrea@mail.com",
              "firstName": "Andrea",
              "lastName": "Perez",
              "phone": "3001234567",
              "city": "Santa Marta",
              "birthDate": "1998-05-10",
              "password": "UnaFraseSeguraLarga2026!"
            }
            """;

    private UserResponse andrea() {
        return new UserResponse(
                1L,
                "andrea",
                "andrea@mail.com",
                true,
                "Andrea",
                "Perez",
                "3001234567",
                "Santa Marta",
                LocalDate.of(1998, 5, 10)
        );
    }

    // TEST-CTRL-USR-001 / AC-CTRL007: registro válido -> 201
    @Test
    void shouldRegisterUser() throws Exception {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(andrea());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@mail.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.firstName").value("Andrea"));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    // phone y city son opcionales: sin ellos también es válido
    @Test
    void shouldRegisterUserWithoutOptionalFields() throws Exception {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(andrea());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "andrea",
                                  "email": "andrea@mail.com",
                                  "firstName": "Andrea",
                                  "lastName": "Perez",
                                  "birthDate": "1998-05-10",
                                  "password": "UnaFraseSeguraLarga2026!"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(userService).register(any(RegisterUserRequest.class));
    }

    // TEST-CTRL-USR-002: email inválido -> 400, Service nunca invocado
    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("andrea@mail.com", "esto-no-es-un-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email format is invalid"));

        verify(userService, never()).register(any());
    }

    // Extra: body vacío -> todos los obligatorios en details
    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").value("Username is required"))
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.details.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.details.birthDate").value("Birth date is required"))
                .andExpect(jsonPath("$.details.password").value("Password is required"));
        verify(userService, never()).register(any());
    }

    // Extra: JSON mal formado -> 400
    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(userService, never()).register(any());
    }

    // TEST-CTRL-USR-003 / AC-CTRL008: username duplicado -> 409 sin detalles internos
    @Test
    void shouldReturn409WhenUsernameAlreadyExists() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    // Extra: email duplicado -> 409
    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists: andrea@mail.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists: andrea@mail.com"));
    }

    // Extra: fecha de nacimiento futura la rechaza el Service (regla de negocio) -> 409
    @Test
    void shouldReturn409WhenBirthDateIsInTheFuture() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new BusinessRuleException("Birth date cannot be in the future: 2999-01-01"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("1998-05-10", "2999-01-01")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Birth date cannot be in the future: 2999-01-01"));
    }

    // TEST-CTRL-USR-004: consultar por email -> 200
    @Test
    void shouldReturnUserByEmail() throws Exception {
        when(userService.findByEmail("andrea@mail.com")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("andrea@mail.com"));

        verify(userService).findByEmail("andrea@mail.com");
    }

    // FR-CTRLUSR-002: email inexistente -> 404
    @Test
    void shouldReturn404WhenEmailDoesNotExist() throws Exception {
        when(userService.findByEmail("nadie@mail.com"))
                .thenThrow(new ResourceNotFoundException("User not found: nadie@mail.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "nadie@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: nadie@mail.com"));
    }

    // Extra: falta ?email= -> 400 (no 500)
    @Test
    void shouldReturn400WhenEmailParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/by-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());

        verify(userService, never()).findByEmail(any());
    }

    // TEST-CTRL-USR-005: consultar por username -> 200
    @Test
    void shouldReturnUserByUsername() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }

    // FR-CTRLUSR-003: username inexistente -> 404
    @Test
    void shouldReturn404WhenUsernameDoesNotExist() throws Exception {
        when(userService.findByUsername("nadie"))
                .thenThrow(new ResourceNotFoundException("User not found: nadie"));

        mockMvc.perform(get("/api/users/by-username").param("username", "nadie"))
                .andExpect(status().isNotFound());
    }
}