package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserCredentialRepository credentialRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    // ------------------------------------------------------- loadUserByUsername

    @Test
    void loadUserByUsername_withAtSign_looksUpByEmail() {
        // ARRANGE
        when(credentialRepository.findByUserEmailIgnoreCase("andrea@mail.com"))
                .thenReturn(Optional.of(credential(user(7L), Set.of(Role.USER))));

        // ACT
        SecurityPrincipal principal = service.loadUserByUsername("andrea@mail.com");

        // ASSERT
        assertThat(principal.getUsername()).isEqualTo("andrea");
        assertThat(principal.getEmail()).isEqualTo("andrea@mail.com");
        assertThat(principal.getPassword()).isEqualTo("hash-bcrypt");
        verify(credentialRepository, never()).findByUserUsername("andrea@mail.com");
    }

    @Test
    void loadUserByUsername_withoutAtSign_looksUpByUsername() {
        // ARRANGE
        when(credentialRepository.findByUserUsername("andrea"))
                .thenReturn(Optional.of(credential(user(7L), Set.of(Role.USER))));

        // ACT
        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        // ASSERT
        assertThat(principal.getId()).isEqualTo(7L);
        verify(credentialRepository, never()).findByUserEmailIgnoreCase("andrea");
    }

    @Test
    void loadUserByUsername_unknownUsername_throwsUsernameNotFound() {
        // ARRANGE
        when(credentialRepository.findByUserUsername("nadie")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFound() {
        // ARRANGE
        when(credentialRepository.findByUserEmailIgnoreCase("nadie@mail.com"))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.loadUserByUsername("nadie@mail.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // --------------------------------------------------------- estado y roles

    @Test
    void principal_ofInactiveUser_isNotEnabled() {
        // ARRANGE
        User inactive = user(7L);
        inactive.deactivate();
        when(credentialRepository.findByUserUsername("andrea"))
                .thenReturn(Optional.of(credential(inactive, Set.of(Role.USER))));

        // ACT
        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        // ASSERT (SEC011: un usuario desactivado pierde acceso)
        assertThat(principal.isEnabled()).isFalse();
    }

    @Test
    void principal_ofActiveUser_isEnabled() {
        // ARRANGE
        when(credentialRepository.findByUserUsername("andrea"))
                .thenReturn(Optional.of(credential(user(7L), Set.of(Role.USER))));

        // ACT + ASSERT
        assertThat(service.loadUserByUsername("andrea").isEnabled()).isTrue();
    }

    @Test
    void principal_rolesBecomeRoleAuthorities() {
        // ARRANGE
        when(credentialRepository.findByUserUsername("andrea"))
                .thenReturn(Optional.of(credential(user(7L), Set.of(Role.USER, Role.STAFF))));

        // ACT
        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        // ASSERT
        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(authorities).containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
    }

    // -------------------------------------------------------------- loadUserById

    @Test
    void loadUserById_existingUser_returnsPrincipalWithCurrentRoles() {
        // ARRANGE
        when(credentialRepository.findByUserId(7L))
                .thenReturn(Optional.of(credential(user(7L), Set.of(Role.ADMIN))));

        // ACT
        SecurityPrincipal principal = service.loadUserById(7L);

        // ASSERT
        assertThat(principal.getId()).isEqualTo(7L);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserById_unknownUser_throwsUsernameNotFound() {
        // ARRANGE
        when(credentialRepository.findByUserId(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.loadUserById(99L))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ----------------------------------------------------------------- helpers

    // El id lo asigna JPA al persistir; en un test unitario lo fijamos por reflexión
    private User user(Long id) {
        User user = new User("andrea", "andrea@mail.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserCredential credential(User user, Set<Role> roles) {
        return new UserCredential(user, "hash-bcrypt", roles, LocalDateTime.of(2026, 10, 19, 12, 0));
    }
}