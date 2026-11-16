package com.pulsepass.controller;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticación: {@code /api/auth} (iniciar sesión y consultar "quién soy").
 *
 * <p>Capa Controller: solo traduce entre HTTP y Java. Lee la petición (ruta, parámetros, cuerpo JSON),
 * delega TODO el trabajo en el servicio y devuelve un {@code ResponseEntity} con el código HTTP y el
 * cuerpo. No contiene reglas de negocio ni habla con los repositorios.</p>
 *
 * <p>Anotaciones habituales: {@code @RestController} (cada método devuelve datos que Spring serializa
 * a JSON), {@code @RequestMapping} (prefijo común de las rutas) y {@code @GetMapping} /
 * {@code @PostMapping} / {@code @PatchMapping} (verbo HTTP + ruta). Los errores no se manejan aquí:
 * las excepciones las traduce {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // AuthService es una clase concreta (no tiene interfaz); se inyecta por constructor.
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/login  ->  200
    // Público. @RequestBody convierte el JSON del cuerpo en un LoginRequest y @Valid ejecuta sus
    // validaciones ANTES de entrar al método (si fallan, 400). Devuelve el token JWT.
    // Credenciales incorrectas -> AuthService lanza BadCredentialsException -> 401.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // GET /api/auth/me  ->  200
    // Requiere token. @AuthenticationPrincipal inyecta el SecurityPrincipal que dejó
    // JwtAuthenticationFilter en el SecurityContext; no viene del cliente. Sin token válido, la petición
    // ni siquiera llega aquí (responde 401).
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(@AuthenticationPrincipal SecurityPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }
}
