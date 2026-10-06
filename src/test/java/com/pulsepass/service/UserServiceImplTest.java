package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    // ---------- TEST-USER-001 ----------
    @Test
    void register_validUser_savesActiveUserWithProfile() {
        // ARRANGE
        RegisterUserRequest request = request("andrea", "andrea@email.com", LocalDate.of(2000, 1, 1));
        UserResponse expected = new UserResponse(1L, "andrea", "andrea@email.com", true,
                "Andrea", "Lopez", "3000000000", "Santa Marta", LocalDate.of(2000, 1, 1));
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        // ACT
        UserResponse result = userService.register(request);

        // ASSERT
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getProfile()).isNotNull();
        assertThat(result).isEqualTo(expected);
    }

    // ---------- TEST-USER-002 ----------
    @Test
    void register_duplicatedUsername_throwsDuplicateResource() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(
                request("andrea", "andrea@email.com", LocalDate.of(2000, 1, 1))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- TEST-USER-003 ----------
    @Test
    void register_duplicatedEmail_throwsDuplicateResource() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(
                request("andrea", "andrea@email.com", LocalDate.of(2000, 1, 1))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- TEST-USER-004 ----------
    @Test
    void register_futureBirthDate_throwsBusinessRule() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(
                request("andrea", "andrea@email.com", LocalDate.now().plusDays(1))))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    private RegisterUserRequest request(String username, String email, LocalDate birthDate) {
        return new RegisterUserRequest(username, email, "Andrea", "Lopez",
                "3000000000", "Santa Marta", birthDate);
    }
}