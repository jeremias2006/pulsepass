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

    @Test
    void isSelfByEmail_sameUser_returnsTrue() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com"))
                .thenReturn(Optional.of(user(7L)));

        assertThat(authorizationService.isSelfByEmail("andrea@mail.com")).isTrue();
    }

    @Test
    void isSelfByEmail_otherUser_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByEmailIgnoreCase("carlos@mail.com"))
                .thenReturn(Optional.of(user(8L)));

        assertThat(authorizationService.isSelfByEmail("carlos@mail.com")).isFalse();
    }

    @Test
    void isSelfByEmail_unknownEmail_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByEmailIgnoreCase("nadie@mail.com")).thenReturn(Optional.empty());

        assertThat(authorizationService.isSelfByEmail("nadie@mail.com")).isFalse();
    }

    @Test
    void isSelfByEmail_withoutAuthentication_returnsFalse() {
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com"))
                .thenReturn(Optional.of(user(7L)));

        assertThat(authorizationService.isSelfByEmail("andrea@mail.com")).isFalse();
    }

    @Test
    void isSelfByEmail_nullEmail_returnsFalseWithoutTouchingRepository() {
        authenticateAs(7L, "ROLE_USER");

        assertThat(authorizationService.isSelfByEmail(null)).isFalse();
        verifyNoInteractions(userRepository);
    }

    // --------------------------------------------------------- isSelfByUsername

    @Test
    void isSelfByUsername_sameUser_returnsTrue() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(7L)));

        assertThat(authorizationService.isSelfByUsername("andrea")).isTrue();
    }

    @Test
    void isSelfByUsername_otherUser_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(user(8L)));

        assertThat(authorizationService.isSelfByUsername("carlos")).isFalse();
    }

    // ------------------------------------------------------------ isTicketOwner

    @Test
    void isTicketOwner_owner_returnsTrue() {
        authenticateAs(7L, "ROLE_USER");
        when(ticketRepository.existsByTicketCodeAndUserId("TKT-A1B2C3D4", 7L)).thenReturn(true);

        assertThat(authorizationService.isTicketOwner("TKT-A1B2C3D4")).isTrue();
    }

    @Test
    void isTicketOwner_thirdParty_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");
        when(ticketRepository.existsByTicketCodeAndUserId("TKT-DE-CARLOS", 7L)).thenReturn(false);

        assertThat(authorizationService.isTicketOwner("TKT-DE-CARLOS")).isFalse();
    }

    @Test
    void isTicketOwner_withoutAuthentication_returnsFalseWithoutTouchingRepository() {
        assertThat(authorizationService.isTicketOwner("TKT-A1B2C3D4")).isFalse();
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void isTicketOwner_nullCode_returnsFalse() {
        authenticateAs(7L, "ROLE_USER");

        assertThat(authorizationService.isTicketOwner(null)).isFalse();
        verifyNoInteractions(ticketRepository);
    }

    // ------------------------------------------------ isAdmin / isStaffOrAdmin

    @Test
    void roles_user_isNeitherAdminNorStaff() {
        authenticateAs(7L, "ROLE_USER");

        assertThat(authorizationService.isAdmin()).isFalse();
        assertThat(authorizationService.isStaffOrAdmin()).isFalse();
    }

    @Test
    void roles_staff_isStaffOrAdminButNotAdmin() {
        authenticateAs(7L, "ROLE_STAFF");

        assertThat(authorizationService.isAdmin()).isFalse();
        assertThat(authorizationService.isStaffOrAdmin()).isTrue();
    }

    @Test
    void roles_admin_isBoth() {
        authenticateAs(7L, "ROLE_ADMIN");

        assertThat(authorizationService.isAdmin()).isTrue();
        assertThat(authorizationService.isStaffOrAdmin()).isTrue();
    }

    @Test
    void roles_withoutAuthentication_isNeither() {
        assertThat(authorizationService.isAdmin()).isFalse();
        assertThat(authorizationService.isStaffOrAdmin()).isFalse();
    }

    // -------------------------------------------------------- currentPrincipal

    @Test
    void currentPrincipal_nonSecurityPrincipal_returnsEmpty() {
        // Una identidad que no es SecurityPrincipal (p. ej. un String) no se acepta
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "anonymousUser", null, List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(authorizationService.currentPrincipal()).isEmpty();
    }

    // ----------------------------------------------------------------- helpers

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

    private User user(Long id) {
        User user = new User("usuario", "usuario@mail.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}