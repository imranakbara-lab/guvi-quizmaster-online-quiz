package com.quizmaster.service;

import com.quizmaster.TestData;
import com.quizmaster.dto.*;
import com.quizmaster.entity.PasswordResetToken;
import com.quizmaster.entity.Role;
import com.quizmaster.entity.User;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.repository.PasswordResetTokenRepository;
import com.quizmaster.repository.UserRepository;
import com.quizmaster.security.JwtService;
import com.quizmaster.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;

    @InjectMocks private AuthService authService;

    @Test
    void register_createsParticipantAndSendsWelcomeEmail() {
        when(userRepository.existsByEmail("ravi@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> { User u = inv.getArgument(0); u.setId(1L); return u; });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt");

        AuthResponse res = authService.register(new RegisterRequest("Ravi", "Ravi@Mail.com", "secret1", null));

        assertThat(res.token()).isEqualTo("jwt");
        assertThat(res.user().role()).isEqualTo(Role.PARTICIPANT);
        verify(emailService).sendWelcome("ravi@mail.com", "Ravi");
    }

    @Test
    void register_duplicateEmail_rejected() {
        when(userRepository.existsByEmail("ravi@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Ravi", "ravi@mail.com", "secret1", null)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(emailService);
    }

    @Test
    void login_wrongPassword_throws() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ravi@mail.com", "x")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void forgotPassword_knownEmail_savesTokenAndSendsEmail() {
        User u = TestData.user(1L, "ravi@mail.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("ravi@mail.com")).thenReturn(Optional.of(u));

        authService.forgotPassword(new ForgotPasswordRequest("ravi@mail.com"));

        ArgumentCaptor<PasswordResetToken> token = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(token.capture());
        assertThat(token.getValue().isValid()).isTrue();
        verify(emailService).sendPasswordReset(eq("ravi@mail.com"), anyString(), eq(token.getValue().getToken()));
    }

    @Test
    void forgotPassword_unknownEmail_sameMessageNoEmail() {
        when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

        MessageResponse res = authService.forgotPassword(new ForgotPasswordRequest("nobody@mail.com"));

        assertThat(res.message()).contains("If this email is registered");
        verifyNoInteractions(emailService);
    }

    @Test
    void resetPassword_validToken_changesPasswordOnce() {
        User u = TestData.user(1L, "ravi@mail.com", Role.PARTICIPANT);
        PasswordResetToken t = new PasswordResetToken("tok", u, LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(t));
        when(passwordEncoder.encode("newpass1")).thenReturn("NEWENC");

        authService.resetPassword(new ResetPasswordRequest("tok", "newpass1"));

        assertThat(u.getPassword()).isEqualTo("NEWENC");
        assertThat(t.isUsed()).isTrue();
    }

    @Test
    void resetPassword_expiredToken_rejected() {
        User u = TestData.user(1L, "ravi@mail.com", Role.PARTICIPANT);
        when(tokenRepository.findByToken("old")).thenReturn(Optional.of(new PasswordResetToken("old", u, LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("old", "newpass1")))
                .isInstanceOf(BadRequestException.class);
    }
}
