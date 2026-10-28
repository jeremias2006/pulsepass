package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Implementación de la lógica de negocio de usuarios ({@link UserService}): reglas BR-USER-001 a 005.
 * {@code @Transactional(readOnly = true)} es el valor por defecto; {@code register} lo sobrescribe
 * porque escribe en la base.
 */
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Registra un usuario y su perfil en una sola transacción.
     *
     * <p>Cuando lo llama {@code AuthService.register} (que ya es transaccional), este método se une a
     * esa misma transacción: si algo falla después, también se revierte el usuario.</p>
     *
     * @throws DuplicateResourceException si el username o el email ya existen (409)
     * @throws BusinessRuleException si la fecha de nacimiento es nula o futura (409)
     */
    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        // BR-USER-001: el username debe ser único.
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }
        // BR-USER-002: el email debe ser único (sin distinguir mayúsculas/minúsculas).
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }
        // BR-USER-005: la fecha de nacimiento es obligatoria y no puede ser futura.
        if (request.birthDate() == null || request.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Birth date cannot be in the future: " + request.birthDate());
        }

        // BR-USER-003: User inicia con active = true (valor por defecto de la entidad)
        // BR-USER-004: User y UserProfile se guardan en la misma transaccion (cascade ALL)
        User user = new User(request.username(), request.email());
        UserProfile profile = new UserProfile(
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.city(),
                request.birthDate());
        // Enlaza los dos lados de la relación 1:1 (user.profile y profile.user) para que JPA los guarde juntos.
        user.assignProfile(profile);

        // save(user) guarda también el perfil (cascade ALL). Se devuelve un DTO sin datos sensibles.
        return userMapper.toResponse(userRepository.save(user));
    }

    // Optional.map convierte la entidad a DTO si existe; si está vacío, orElseThrow lanza 404.
    @Override
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    // Mismo patrón que findByEmail, pero buscando por username.
    @Override
    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}