package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.impl.AuthorizationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de AuthorizationServiceImpl (las reglas "¿es el propietario?" y "¿tiene este rol?").
 *
 * No hay HTTP ni JWT: cada prueba simula una petición autenticada colocando un SecurityPrincipal en el SecurityContextHolder
 * (ver {@code authenticateAs}). Los repositorios son dobles de Mockito. {@code verifyNoInteractions(repo)} comprueba que, ante
 * una entrada nula o sin autenticación, el servicio responde false SIN consultar la base.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private AuthorizationServiceImpl authorizationService;

    // El contexto de seguridad es estático: hay que limpiarlo tras cada test
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------ isSelfByEmail

    // El email pertenece al usuario autenticado (mismo id): true.
    @Test
    void isSelfByEmail_sameUser_returnsTrue() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com"))
                .thenReturn(Optional.of(user(7L)));

        assertThat(authorizationService.isSelfByEmail("andrea@mail.com")).isTrue();
    }

    // El email es de otro usuario (id distinto): false.
    @Test
    void isSelfByEmail_otherUser_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByEmailIgnoreCase("carlos@mail.com"))
                .thenReturn(Optional.of(user(8L)));

        assertThat(authorizationService.isSelfByEmail("carlos@mail.com")).isFalse();
    }

    // El email no existe: false (no se distingue de "es de otro", para no revelar si existe).
    @Test
    void isSelfByEmail_unknownEmail_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByEmailIgnoreCase("nadie@mail.com")).thenReturn(Optional.empty());

        assertThat(authorizationService.isSelfByEmail("nadie@mail.com")).isFalse();
    }

    // Nadie autenticado: false aunque el usuario exista.
    @Test
    void isSelfByEmail_withoutAuthentication_returnsFalse() {
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com"))
                .thenReturn(Optional.of(user(7L)));

        assertThat(authorizationService.isSelfByEmail("andrea@mail.com")).isFalse();
    }

    // Email nulo: false y sin consultar el repositorio.
    @Test
    void isSelfByEmail_nullEmail_returnsFalseWithoutTouchingRepository() {
        authenticateAs(7L, "ROLE_USER");

        assertThat(authorizationService.isSelfByEmail(null)).isFalse();
        verifyNoInteractions(userRepository);
    }

    // --------------------------------------------------------- isSelfByUsername

    // Igual que isSelfByEmail, pero buscando por username: es el propio usuario.
    @Test
    void isSelfByUsername_sameUser_returnsTrue() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(7L)));

        assertThat(authorizationService.isSelfByUsername("andrea")).isTrue();
    }

    // Por username: es otro usuario.
    @Test
    void isSelfByUsername_otherUser_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(user(8L)));

        assertThat(authorizationService.isSelfByUsername("carlos")).isFalse();
    }

    // ------------------------------------------------------------ isTicketOwner

    // El repositorio confirma que el ticket es del usuario autenticado: true.
    @Test
    void isTicketOwner_owner_returnsTrue() {
        authenticateAs(7L, "ROLE_USER");
        when(ticketRepository.existsByTicketCodeAndUserId("TKT-A1B2C3D4", 7L)).thenReturn(true);

        assertThat(authorizationService.isTicketOwner("TKT-A1B2C3D4")).isTrue();
    }

    // El ticket es de otra persona: false.
    @Test
    void isTicketOwner_thirdParty_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");
        when(ticketRepository.existsByTicketCodeAndUserId("TKT-DE-CARLOS", 7L)).thenReturn(false);

        assertThat(authorizationService.isTicketOwner("TKT-DE-CARLOS")).isFalse();
    }

    // Sin autenticación: false y sin consultar el repositorio.
    @Test
    void isTicketOwner_withoutAuthentication_returnsFalseWithoutTouchingRepository() {
        assertThat(authorizationService.isTicketOwner("TKT-A1B2C3D4")).isFalse();
        verifyNoInteractions(ticketRepository);
    }

    // Código de ticket nulo: false y sin consultar el repositorio.
    @Test
    void isTicketOwner_nullCode_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");

        assertThat(authorizationService.isTicketOwner(null)).isFalse();
        verifyNoInteractions(ticketRepository);
    }

    // ------------------------------------------------ isAdmin / isStaffOrAdmin

    // Un USER no es ADMIN ni STAFF.
    @Test
    void roles_user_isNeitherAdminNorStaff() {
        authenticateAs(7L, "ROLE_USER");

        assertThat(authorizationService.isAdmin()).isFalse();
        assertThat(authorizationService.isStaffOrAdmin()).isFalse();
    }

    // Un STAFF cuenta como "staff o admin", pero no como ADMIN.
    @Test
    void roles_staff_isStaffOrAdminButNotAdmin() {
        authenticateAs(7L, "ROLE_STAFF");

        assertThat(authorizationService.isAdmin()).isFalse();
        assertThat(authorizationService.isStaffOrAdmin()).isTrue();
    }

    // Un ADMIN es ADMIN y también cuenta como "staff o admin".
    @Test
    void roles_admin_isBoth() {
        authenticateAs(7L, "ROLE_ADMIN");

        assertThat(authorizationService.isAdmin()).isTrue();
        assertThat(authorizationService.isStaffOrAdmin()).isTrue();
    }

    // Sin autenticación no se tiene ningún rol.
    @Test
    void roles_withoutAuthentication_isNeither() {
        assertThat(authorizationService.isAdmin()).isFalse();
        assertThat(authorizationService.isStaffOrAdmin()).isFalse();
    }

    // -------------------------------------------------------- currentPrincipal

    // Cubre al usuario anónimo de Spring, cuyo principal es el texto "anonymousUser" (no un SecurityPrincipal): resultado vacío.
    @Test
    void currentPrincipal_nonSecurityPrincipal_returnsEmpty() {
        // Una identidad que no es SecurityPrincipal (p. ej. un String) no se acepta
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "anonymousUser", null, List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(authorizationService.currentPrincipal()).isEmpty();
    }

    // ----------------------------------------------------------------- helpers

    // Simula una petición autenticada: crea un SecurityPrincipal con el id y los roles dados y lo deja en el SecurityContext.
    private void authenticateAs(Long userId, String... authorities) {
        List<SimpleGrantedAuthority> granted = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityPrincipal principal = new SecurityPrincipal(
                userId, "andrea", "andrea@mail.com", "hash", true, granted);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
    }

    // Crea un User con el id indicado. El id lo asigna JPA al persistir; en una prueba unitaria se fija por reflexión.
    private User user(Long id) {
        User user = new User("usuario", "usuario@mail.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}