package com.quizmaster.service;

import com.quizmaster.TestData;
import com.quizmaster.dto.UserUpdateRequest;
import com.quizmaster.entity.Role;
import com.quizmaster.entity.User;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.repository.PasswordResetTokenRepository;
import com.quizmaster.repository.QuizAttemptRepository;
import com.quizmaster.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;

    @InjectMocks private UserService userService;

    @Test
    void update_promotesToAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestData.user(1L, "a@mail.com", Role.PARTICIPANT)));
        when(userRepository.findByEmail("a@mail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(userService.update(1L, new UserUpdateRequest("A", "a@mail.com", null, Role.ADMIN)).role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void delete_removesAttemptsAndTokens() {
        User u = TestData.user(1L, "a@mail.com", Role.PARTICIPANT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        userService.delete(1L, 99L);

        verify(attemptRepository).deleteByUserId(1L);
        verify(tokenRepository).deleteByUserId(1L);
        verify(userRepository).delete(u);
    }

    @Test
    void delete_ownAccount_rejected() {
        assertThatThrownBy(() -> userService.delete(99L, 99L)).isInstanceOf(BadRequestException.class);
    }
}
