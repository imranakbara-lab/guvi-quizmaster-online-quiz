package com.quizmaster.service;

import com.quizmaster.dto.UserResponse;
import com.quizmaster.dto.UserUpdateRequest;
import com.quizmaster.entity.User;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.exception.ResourceNotFoundException;
import com.quizmaster.repository.PasswordResetTokenRepository;
import com.quizmaster.repository.QuizAttemptRepository;
import com.quizmaster.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final QuizAttemptRepository attemptRepository;
    private final PasswordResetTokenRepository tokenRepository;

    public UserService(UserRepository userRepository, QuizAttemptRepository attemptRepository,
                       PasswordResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.tokenRepository = tokenRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req) {
        User user = find(id);
        String email = req.email().trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new BadRequestException("Email is already used by another user"); });
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        user.setRole(req.role());
        return UserResponse.from(userRepository.save(user));
    }

    /** Deletes the user and all of their quiz attempts. An admin cannot delete their own account. */
    @Transactional
    public void delete(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) throw new BadRequestException("You cannot delete your own account");
        User user = find(id);
        attemptRepository.deleteByUserId(user.getId());
        tokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }
}
