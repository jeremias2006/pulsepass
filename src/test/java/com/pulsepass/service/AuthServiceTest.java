package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "UnaFraseSeguraLarga2026!";

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCredentialRepository credentialRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    // BCrypt real (no mock): así comprobamos que se guarda un hash de verdad
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, userRepository, credentialRepository,
                passwordEncoder, authenticationManager, jwtService, clock);
    }

    // ---------------------------------------------------------------- register

    @Test
    void register_savesBcryptHashAndOnlyUserRole() {
        // ARRANGE
        User user = new User("andrea", "andrea@mail.com");
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(andreaResponse());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // ACT
        UserResponse result = authService.register(registerRequest());

        // ASSERT
        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).save(captor.capture());
        UserCredential saved = captor.getValue();

        assertThat(saved.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(saved.getRoles()).containsExactly(Role.USER);
        assertThat(saved.getPasswordChangedAt()).isEqualTo(LocalDateTime.of(2026, 10, 19, 12, 0));
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void register_whenCredentialSaveFails_propagatesException() {
        // ARRANGE
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(andreaResponse());
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("andrea", "andrea@mail.com")));
        when(credentialRepository.save(any(UserCredential.class)))
                .thenThrow(new IllegalStateException("db down"));

        // ACT + ASSERT (al propagarse, @Transactional revierte también el usuario)
        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
    }

    @Test
    void register_whenUserNotFoundAfterCreation_throwsAndNeverSavesCredential() {
        // ARRANGE
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(andreaResponse());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(IllegalStateException.class);
        verify(credentialRepository, never()).save(any(UserCredential.class));
    }

    // ------------------------------------------------------------------- login

    @Test
    void login_success_returnsBearerTokenWith900Seconds() {
        // ARRANGE
        SecurityPrincipal principal = principal();
        Authentication authenticated = UsernamePasswordAuthenticationToken
                .authenticated(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(jwtService.generateAccessToken(7L)).thenReturn("jwt-de-prueba");
        when(jwtService.getExpiresInSeconds()).thenReturn(900L);

        // ACT
        AuthResponse result = authService.login(new LoginRequest("andrea", RAW_PASSWORD));

        // ASSERT
        assertThat(result.accessToken()).isEqualTo("jwt-de-prueba");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(900L);
    }

    @Test
    void login_trimsIdentifierBeforeAuthenticating() {
        // ARRANGE
        SecurityPrincipal principal = principal();
        Authentication authenticated = UsernamePasswordAuthenticationToken
                .authenticated(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(jwtService.generateAccessToken(7L)).thenReturn("jwt-de-prueba");
        when(jwtService.getExpiresInSeconds()).thenReturn(900L);

        // ACT
        authService.login(new LoginRequest("  andrea  ", RAW_PASSWORD));

        // ASSERT
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("andrea");
    }

    // Contraseña mala, cuenta inactiva o usuario inexistente: SIEMPRE el mismo error genérico
    @ParameterizedTest
    @MethodSource("authenticationFailures")
    void login_anyAuthenticationFailure_throwsGenericBadCredentialsAndNeverIssuesToken(
            AuthenticationException failure) {
        // ARRANGE
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(failure);

        // ACT + ASSERT
        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", "cualquiera")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        verifyNoInteractions(jwtService);
    }

    static Stream<AuthenticationException> authenticationFailures() {
        return Stream.of(
                new BadCredentialsException("bad"),
                new DisabledException("disabled"),
                new UsernameNotFoundException("not found"));
    }

    // ----------------------------------------------------------------- helpers

    private RegisterUserRequest registerRequest() {
        return new RegisterUserRequest("andrea", "andrea@mail.com", "Andrea", "Perez",
                "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10), RAW_PASSWORD);
    }

    private UserResponse andreaResponse() {
        return new UserResponse(1L, "andrea", "andrea@mail.com", true, "Andrea", "Perez",
                "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10));
    }

    private SecurityPrincipal principal() {
        return new SecurityPrincipal(7L, "andrea", "andrea@mail.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}