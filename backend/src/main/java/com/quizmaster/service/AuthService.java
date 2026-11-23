package com.quizmaster.service;

import com.quizmaster.dto.*;
import com.quizmaster.entity.PasswordResetToken;
import com.quizmaster.entity.Role;
import com.quizmaster.entity.User;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.repository.PasswordResetTokenRepository;
import com.quizmaster.repository.UserRepository;
import com.quizmaster.security.JwtService;
import com.quizmaster.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       AuthenticationManager authenticationManager, EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    /** Public sign-up always creates a PARTICIPANT. Admins are created by the seeder or promoted by an admin. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }
        User user = new User(req.name().trim(), email, passwordEncoder.encode(req.password()),
                blankToNull(req.phone()), Role.PARTICIPANT);
        User saved = userRepository.save(user);
        emailService.sendWelcome(saved.getEmail(), saved.getName());
        return new AuthResponse(jwtService.generateToken(UserPrincipal.from(saved)), UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(jwtService.generateToken(UserPrincipal.from(user)), UserResponse.from(user));
    }

    /** Always answers the same way, so nobody can find out which emails are registered. */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.email().trim().toLowerCase()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            tokenRepository.save(new PasswordResetToken(token, user, LocalDateTime.now().plusMinutes(30)));
            emailService.sendPasswordReset(user.getEmail(), user.getName(), token);
        });
        return new MessageResponse("If this email is registered, a reset link has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        PasswordResetToken token = tokenRepository.findByToken(req.token())
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has expired"));
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        token.setUsed(true);
        tokenRepository.save(token);
        return new MessageResponse("Password updated. You can now log in.");
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
