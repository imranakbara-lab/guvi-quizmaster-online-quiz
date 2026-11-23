package com.quizmaster.repository;

import com.quizmaster.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository tests on an in-memory H2 database. */
@DataJpaTest
class QuizRepositoryTest {

    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private UserRepository userRepository;

    private Quiz quiz(String title, boolean published, int questions) {
        Quiz q = new Quiz();
        q.setTitle(title);
        q.setTimeLimitMinutes(5);
        q.setPublished(published);
        for (int i = 0; i < questions; i++) {
            Question question = new Question();
            question.setText("Q" + i);
            question.addOption(new AnswerOption("yes", true));
            question.addOption(new AnswerOption("no", false));
            q.addQuestion(question);
        }
        return quizRepository.save(q);
    }

    @Test
    void findAvailable_onlyPublishedWithQuestions() {
        quiz("Published", true, 2);
        quiz("Draft", false, 2);
        quiz("Empty", true, 0);

        assertThat(quizRepository.findAvailable()).extracting(Quiz::getTitle).containsExactly("Published");
    }

    @Test
    void cascadeSavesQuestionsAndOptions() {
        Quiz saved = quiz("Cascade", true, 3);

        Quiz loaded = quizRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getQuestions()).hasSize(3);
        assertThat(loaded.getQuestions().get(0).getOptions()).hasSize(2);
        assertThat(loaded.getQuestions().get(0).correctOptionIds()).hasSize(1);
    }

    @Test
    void attemptQueries_filterByUserAndStatus() {
        Quiz q = quiz("Java", true, 1);
        User u = userRepository.save(new User("P", "p@mail.com", "x", null, Role.PARTICIPANT));

        QuizAttempt done = new QuizAttempt();
        done.setUser(u);
        done.setQuiz(q);
        done.setStartedAt(LocalDateTime.now().minusMinutes(5));
        done.setExpiresAt(LocalDateTime.now());
        done.setSubmittedAt(LocalDateTime.now());
        done.setStatus(AttemptStatus.SUBMITTED);
        done.setScore(1);
        done.setTotalQuestions(1);
        attemptRepository.save(done);

        QuizAttempt running = new QuizAttempt();
        running.setUser(u);
        running.setQuiz(q);
        running.setStartedAt(LocalDateTime.now());
        running.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        attemptRepository.save(running);

        assertThat(attemptRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(u.getId(), AttemptStatus.SUBMITTED)).hasSize(1);
        assertThat(attemptRepository.findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(u.getId(), q.getId(), AttemptStatus.IN_PROGRESS))
                .get().extracting(QuizAttempt::getId).isEqualTo(running.getId());

        attemptRepository.deleteByUserId(u.getId());
        assertThat(attemptRepository.count()).isZero();
    }
}
