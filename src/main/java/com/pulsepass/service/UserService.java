package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Contrato de la lógica de negocio de usuarios: registrar y consultar.
 * La implementación es {@code UserServiceImpl}. Las consultas se protegen con {@code @PreAuthorize}:
 * un usuario solo puede verse a sí mismo; el ADMIN puede ver a cualquiera.
 */
public interface UserService {

    /**
     * Registra un usuario con su perfil. Reglas de negocio: BR-USER-001 a 005.
     * No maneja contraseñas: de eso se encarga {@code AuthService}, que llama a este método y luego
     * guarda la credencial.
     *
     * <p>Público (lo invoca AuthService durante el registro): SIN @PreAuthorize.</p>
     */
    UserResponse register(RegisterUserRequest request);

    /** Busca un usuario por email. FR-SECUSR-02: ADMIN, o el propio usuario. */
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isSelfByEmail(#email)")
    UserResponse findByEmail(String email);

    /** Busca un usuario por username. FR-SECUSR-03: ADMIN, o el propio usuario. */
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isSelfByUsername(#username)")
    UserResponse findByUsername(String username);
}
