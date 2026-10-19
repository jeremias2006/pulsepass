package com.pulsepass.security;

import com.pulsepass.domain.UserCredential;
import com.pulsepass.repository.UserCredentialRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserCredentialRepository credentialRepository;

    public CustomUserDetailsService(UserCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    /** Login: el identificador puede ser email (contiene "@") o username. */
    @Override
    public SecurityPrincipal loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Optional<UserCredential> found = identifier.contains("@")
                ? credentialRepository.findByUserEmailIgnoreCase(identifier)
                : credentialRepository.findByUserUsername(identifier);

        return found.map(SecurityPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Credentials not found"));
    }

    /** Filtro JWT: revalida usuario, estado y roles actuales en CADA request. */
    public SecurityPrincipal loadUserById(Long userId) throws UsernameNotFoundException {
        return credentialRepository.findByUserId(userId)
                .map(SecurityPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Credentials not found"));
    }
}