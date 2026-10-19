package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserService {

    // Público (lo invoca AuthService durante el registro): SIN @PreAuthorize
    UserResponse register(RegisterUserRequest request);

    // FR-SECUSR-02
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isSelfByEmail(#email)")
    UserResponse findByEmail(String email);

    // FR-SECUSR-03
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isSelfByUsername(#username)")
    UserResponse findByUsername(String username);
}
