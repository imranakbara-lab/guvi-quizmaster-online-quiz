package com.quizmaster.repository;

import com.quizmaster.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /** Published quizzes that have at least one question, newest first. */
    @Query("select q from Quiz q where q.published = true and size(q.questions) > 0 order by q.createdAt desc")
    List<Quiz> findAvailable();

    List<Quiz> findAllByOrderByCreatedAtDesc();
}
