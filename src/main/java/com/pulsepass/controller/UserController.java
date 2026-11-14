package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de usuarios: {@code /api/users} (registro público y consultas protegidas).
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
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;   // consultas (por email o username)
    private final AuthService authService;   // registro: además del usuario crea su credencial

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // POST /api/users  ->  201 Created
    // Público (cualquiera puede registrarse). Se usa authService.register y no userService.register
    // porque el registro completo incluye guardar la contraseña como hash BCrypt con el rol USER.
    @PostMapping
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterUserRequest request) {

        UserResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET /api/users/by-email?email=andrea@mail.com
    // ADMIN, o el propio usuario (FR-SECUSR-02).
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(
            @RequestParam String email) {

        return ResponseEntity.ok(userService.findByEmail(email));
    }

    // GET /api/users/by-username?username=andrea
    // ADMIN, o el propio usuario (FR-SECUSR-03).
    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(
            @RequestParam String username) {

        return ResponseEntity.ok(userService.findByUsername(username));
    }
}