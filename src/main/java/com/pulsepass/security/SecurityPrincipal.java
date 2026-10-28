package com.pulsepass.security;

import com.pulsepass.domain.UserCredential;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Representa al usuario autenticado dentro de Spring Security.
 *
 * <p>Implementa {@code UserDetails}, la interfaz que Spring Security entiende. Es un adaptador:
 * toma los datos de nuestro dominio ({@code UserCredential} y su {@code User}) y los expone en el
 * formato que Spring necesita. Una vez autenticada la petición, este objeto queda guardado como
 * "principal" en el {@code SecurityContext}, y de ahí lo lee {@code AuthorizationServiceImpl}.</p>
 *
 * <p>Es inmutable (todos los campos son final y la lista de roles se copia).</p>
 */
public class SecurityPrincipal implements UserDetails {

    private final Long id;                    // id del User (el mismo que va en el claim "sub" del JWT)
    private final String username;
    private final String email;
    private final String passwordHash;        // hash BCrypt; nunca la contraseña en texto plano
    private final boolean enabled;            // refleja User.active
    private final List<SimpleGrantedAuthority> authorities;   // roles con prefijo "ROLE_"

    public SecurityPrincipal(Long id, String username, String email, String passwordHash,
                             boolean enabled, List<SimpleGrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        // Copia inmutable: nadie puede modificar los roles después de construir el principal.
        this.authorities = List.copyOf(authorities);
    }

    // Debe llamarse dentro de una transacción (roles y user son LAZY)
    // Fábrica: convierte una UserCredential en SecurityPrincipal. Cada Role (USER, STAFF, ADMIN) se
    // convierte en una autoridad con prefijo "ROLE_", que es la convención que esperan hasRole('...')
    // y hasAnyRole(...). Por eso CustomUserDetailsService la invoca dentro de su método @Transactional.
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

    // Spring Security compara la contraseña recibida en el login contra este hash.
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // Las tres banderas siguientes están fijas en true: el dominio no maneja cuentas que expiren,
    // se bloqueen ni credenciales que caduquen. Lo único que puede deshabilitar una cuenta es isEnabled().
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

    // toString no incluye el hash: así no se filtra por accidente a los logs.
    @Override
    public String toString() {
        return "SecurityPrincipal[id=" + id + ", username=" + username + "]";   // sin hash
    }
}