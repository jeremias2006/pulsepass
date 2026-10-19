package com.pulsepass.security;

import com.pulsepass.domain.UserCredential;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final List<SimpleGrantedAuthority> authorities;

    public SecurityPrincipal(Long id, String username, String email, String passwordHash,
                             boolean enabled, List<SimpleGrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = List.copyOf(authorities);
    }

    // Debe llamarse dentro de una transacción (roles y user son LAZY)
    public static SecurityPrincipal from(UserCredential credential) {
        List<SimpleGrantedAuthority> authorities = credential.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        return new SecurityPrincipal(
                credential.getUser().getId(),
                credential.getUser().getUsername(),
                credential.getUser().getEmail(),
                credential.getPasswordHash(),
                credential.getUser().isActive(),
                authorities);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;   // User.active (SEC011)
    }

    @Override
    public String toString() {
        return "SecurityPrincipal[id=" + id + ", username=" + username + "]";   // sin hash
    }
}