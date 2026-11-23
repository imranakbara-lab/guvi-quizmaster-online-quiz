package com.quizmaster.repository;

import com.quizmaster.entity.AttemptStatus;
import com.quizmaster.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserIdAndStatusOrderBySubmittedAtDesc(Long userId, AttemptStatus status);
    List<QuizAttempt> findByStatusOrderBySubmittedAtDesc(AttemptStatus status);
    List<QuizAttempt> findByQuizIdAndStatusOrderByScoreDescSubmittedAtAsc(Long quizId, AttemptStatus status);
    Optional<QuizAttempt> findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(Long userId, Long quizId, AttemptStatus status);
    void deleteByUserId(Long userId);
    void deleteByQuizId(Long quizId);
}
