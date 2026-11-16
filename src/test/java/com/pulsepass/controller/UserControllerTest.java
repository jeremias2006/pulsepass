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

import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityErrorWriter;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import org.springframework.security.test.context.support.WithMockUser;

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

/**
 * Prueba de la CAPA WEB (HTTP) del controlador, sin base de datos ni lógica de negocio real. Se envían peticiones HTTP simuladas
 * con {@code MockMvc} y se comprueba la ruta, el código de respuesta, el JSON devuelto, las validaciones ({@code @Valid}) y cómo
 * {@code GlobalExceptionHandler} traduce las excepciones.
 *
 * <ul>
 *   <li>{@code @WebMvcTest}: levanta solo Spring MVC y seguridad, y únicamente el controlador indicado.</li>
 *   <li>{@code @Import}: agrega lo que @WebMvcTest no carga solo: GlobalExceptionHandler, SecurityConfig y los manejadores de
 *       errores 401/403.</li>
 *   <li>{@code @MockitoBean}: reemplaza el servicio por un doble. {@code when(...).thenReturn(...)} o {@code thenThrow(...)} define
 *       lo que "responde" el servicio, así solo se prueba el controlador. JwtService, CustomUserDetailsService y
 *       AuthorizationService también se simulan: son dependencias de la seguridad y así el contexto arranca sin el resto de la aplicación.</li>
 *   <li>{@code jsonPath("$.campo")}: lee un campo del JSON de respuesta. {@code verify(servicio, never())} demuestra que el
 *       controlador rechazó la petición ANTES de llegar al servicio.</li>
 * </ul>
 *
 * El registro (POST /api/users) es público y pasa por AuthService; las consultas por email y username exigen estar autenticado
 * ({@code @WithMockUser}). La regla fina "solo el propio usuario o ADMIN" es un @PreAuthorize del servicio, que aquí está simulado.
 */
@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SecurityErrorWriter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;      // sigue haciendo falta: by-email y by-username
    @MockitoBean
    private AuthService authService;      // NUEVO: register ahora pasa por aquí

    // SecurityConfig los necesita para construir la cadena de filtros
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private AuthorizationService authorizationService;

    // Cuerpo JSON de registro válido; las pruebas de error lo modifican con replace(...).
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

    // Usuario de ejemplo que devuelve el servicio simulado.
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
    // Registro válido: 201 Created con los datos del usuario, y se llamó a authService.register.
    @Test
    void shouldRegisterUser() throws Exception {
        when(authService.register(any(RegisterUserRequest.class))).thenReturn(andrea());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@mail.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.firstName").value("Andrea"));

        verify(authService).register(any(RegisterUserRequest.class));
    }

    // phone y city son opcionales: sin ellos también es válido
    // Teléfono y ciudad son opcionales: sin ellos el registro sigue siendo válido (201).
    @Test
    void shouldRegisterUserWithoutOptionalFields() throws Exception {
        when(authService.register(any(RegisterUserRequest.class))).thenReturn(andrea());

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

        verify(authService).register(any(RegisterUserRequest.class));
    }

    // TEST-CTRL-USR-002: email inválido -> 400, Service nunca invocado
    // Email con formato inválido: 400 "Validation failed" y details.email con el motivo. El servicio nunca se invoca.
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

        verify(authService, never()).register(any());
    }

    // Extra: body vacío -> todos los obligatorios en details
    // Cuerpo vacío: 400 y details trae un mensaje por cada campo obligatorio faltante.
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
        verify(authService, never()).register(any());
    }

    // Extra: JSON mal formado -> 400
    // JSON roto: 400 con el mensaje genérico "Malformed or invalid JSON request".
    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(authService, never()).register(any());
    }

    // TEST-CTRL-USR-003 / AC-CTRL008: username duplicado -> 409 sin detalles internos
    // Username repetido (BR-USER-001): el servicio lanza DuplicateResourceException -> 409. Comprueba que el cuerpo no contiene
    // la palabra "Exception" (no se filtran nombres de clases internas).
    @Test
    void shouldReturn409WhenUsernameAlreadyExists() throws Exception {
        when(authService.register(any(RegisterUserRequest.class)))
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
    // Email repetido (BR-USER-002): 409 con el mensaje de la excepción.
    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists: andrea@mail.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists: andrea@mail.com"));
    }

    // Extra: fecha de nacimiento futura la rechaza el Service (regla de negocio) -> 409
    // Fecha de nacimiento futura: es una regla de negocio del servicio (BR-USER-005), no del controlador -> 409.
    @Test
    void shouldReturn409WhenBirthDateIsInTheFuture() throws Exception {
        when(authService.register(any(RegisterUserRequest.class)))
                .thenThrow(new BusinessRuleException("Birth date cannot be in the future: 2999-01-01"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("1998-05-10", "2999-01-01")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Birth date cannot be in the future: 2999-01-01"));
    }

    // TEST-CTRL-USR-004: consultar por email -> 200
    // Consulta por email (usuario autenticado): 200 y se pasa el email al servicio.
    @Test
    @WithMockUser
    void shouldReturnUserByEmail() throws Exception {
        when(userService.findByEmail("andrea@mail.com")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("andrea@mail.com"));

        verify(userService).findByEmail("andrea@mail.com");
    }

    // FR-CTRLUSR-002: email inexistente -> 404
    // Email inexistente: 404 con el mensaje del servicio.
    @Test
    @WithMockUser
    void shouldReturn404WhenEmailDoesNotExist() throws Exception {
        when(userService.findByEmail("nadie@mail.com"))
                .thenThrow(new ResourceNotFoundException("User not found: nadie@mail.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "nadie@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: nadie@mail.com"));
    }

    // Extra: falta ?email= -> 400 (no 500)
    // Falta el parámetro ?email=: 400 (no 500) y details.email lo indica. El servicio nunca se invoca.
    @Test
    @WithMockUser
    void shouldReturn400WhenEmailParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/by-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());

        verify(userService, never()).findByEmail(any());
    }

    // TEST-CTRL-USR-005: consultar por username -> 200
    // Consulta por username: 200 y se pasa el username al servicio.
    @Test
    @WithMockUser
    void shouldReturnUserByUsername() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }

    // FR-CTRLUSR-003: username inexistente -> 404
    // Username inexistente: 404.
    @Test
    @WithMockUser
    void shouldReturn404WhenUsernameDoesNotExist() throws Exception {
        when(userService.findByUsername("nadie"))
                .thenThrow(new ResourceNotFoundException("User not found: nadie"));

        mockMvc.perform(get("/api/users/by-username").param("username", "nadie"))
                .andExpect(status().isNotFound());
    }

    // Password demasiado corta (< 12) -> 400, AuthService nunca invocado
    // Contraseña de menos de 12 caracteres: 400 con el mensaje de validación del campo password.
    @Test
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("UnaFraseSeguraLarga2026!", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.password")
                        .value("Password must be between 12 and 72 characters"));

        verify(authService, never()).register(any());
    }

    // 40 caracteres "ñ" = 80 bytes: pasa @Size (caracteres) pero excede el límite de BCrypt -> 400
    // La "ñ" ocupa 2 bytes en UTF-8: 40 caracteres son 80 bytes. Cumple el máximo de 72 CARACTERES (@Size) pero excede los
    // 72 BYTES que BCrypt admite, y la validación @AssertTrue del request lo rechaza con 400.
    @Test
    void shouldReturn400WhenPasswordExceedsBcryptBytes() throws Exception {
        String passwordOver72Bytes = "ñ".repeat(40);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("UnaFraseSeguraLarga2026!", passwordOver72Bytes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(authService, never()).register(any());
    }
}