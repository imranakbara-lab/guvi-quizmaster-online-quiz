package com.quizmaster.service;

import com.quizmaster.TestData;
import com.quizmaster.dto.*;
import com.quizmaster.entity.*;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.exception.ForbiddenException;
import com.quizmaster.repository.QuizAttemptRepository;
import com.quizmaster.repository.QuizRepository;
import com.quizmaster.repository.UserRepository;
import com.quizmaster.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 10, 10, 0, 0);

    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    private AttemptService service;
    private Quiz quiz;
    private User participant;
    private final UserPrincipal me = new UserPrincipal(5L, "p@mail.com", "x", "PARTICIPANT");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new AttemptService(attemptRepository, quizRepository, userRepository, emailService, clock);
        quiz = TestData.quiz();
        participant = TestData.user(5L, "p@mail.com", Role.PARTICIPANT);
    }

    private QuizAttempt attempt(LocalDateTime startedAt) {
        QuizAttempt a = new QuizAttempt();
        a.setId(50L);
        a.setUser(participant);
        a.setQuiz(quiz);
        a.setStartedAt(startedAt);
        a.setExpiresAt(startedAt.plusMinutes(quiz.getTimeLimitMinutes()));
        a.setTotalQuestions(2);
        return a;
    }

    @Test
    void start_createsAttemptWithTimerAndHidesAnswers() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(attemptRepository.findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(5L, 1L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(userRepository.findById(5L)).thenReturn(Optional.of(participant));
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            a.setId(50L);
            return a;
        });

        AttemptStartResponse res = service.start(1L, me);

        assertThat(res.attemptId()).isEqualTo(50L);
        assertThat(res.remainingSeconds()).isEqualTo(600);
        assertThat(res.questions()).hasSize(2);
        assertThat(res.questions().get(1).multipleCorrect()).isTrue();
        assertThat(res.questions().get(0).options()).extracting(OptionView::id).containsExactly(100L, 101L);
    }

    @Test
    void start_resumesRunningAttempt() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(attemptRepository.findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(5L, 1L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt(NOW.minusMinutes(4))));

        AttemptStartResponse res = service.start(1L, me);

        assertThat(res.attemptId()).isEqualTo(50L);
        assertThat(res.remainingSeconds()).isEqualTo(360);
        verify(attemptRepository, never()).save(argThat(a -> a.getId() == null));
    }

    @Test
    void start_unpublishedQuiz_rejected() {
        quiz.setPublished(false);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> service.start(1L, me)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void submit_gradesExactMatchOnly() {
        QuizAttempt a = attempt(NOW.minusMinutes(3));
        when(attemptRepository.findById(50L)).thenReturn(Optional.of(a));

        AttemptResult res = service.submit(50L, new SubmitRequest(List.of(
                new AnswerRequest(10L, List.of(100L)),            // correct
                new AnswerRequest(20L, List.of(200L)))), me);      // only one of two correct -> wrong

        assertThat(res.summary().score()).isEqualTo(1);
        assertThat(res.summary().totalQuestions()).isEqualTo(2);
        assertThat(res.summary().percentage()).isEqualTo(50.0);
        assertThat(res.summary().timedOut()).isFalse();
        assertThat(res.review()).extracting(ReviewItem::correct).containsExactly(true, false);
        assertThat(a.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        verify(emailService).sendResult(eq("p@mail.com"), anyString(), eq("Java Basics"), eq(1), eq(2), eq(50.0), eq(false), eq(50L));
    }

    @Test
    void submit_allCorrectIncludingMultiSelect_fullMarks() {
        QuizAttempt a = attempt(NOW.minusMinutes(3));
        when(attemptRepository.findById(50L)).thenReturn(Optional.of(a));

        AttemptResult res = service.submit(50L, new SubmitRequest(List.of(
                new AnswerRequest(10L, List.of(100L)),
                new AnswerRequest(20L, List.of(201L, 200L)))), me);

        assertThat(res.summary().score()).isEqualTo(2);
        assertThat(res.summary().percentage()).isEqualTo(100.0);
    }

    @Test
    void submit_ignoresUnknownOptionsAndSkippedQuestions() {
        QuizAttempt a = attempt(NOW.minusMinutes(3));
        when(attemptRepository.findById(50L)).thenReturn(Optional.of(a));

        AttemptResult res = service.submit(50L, new SubmitRequest(List.of(new AnswerRequest(10L, List.of(999L)))), me);

        assertThat(res.summary().score()).isZero();
        assertThat(res.review()).hasSize(2);
        assertThat(res.review().get(0).selectedOptionIds()).isEmpty();
    }

    @Test
    void submit_afterTimeLimit_isMarkedTimedOut() {
        QuizAttempt a = attempt(NOW.minusMinutes(20));     // expired 10 minutes ago
        when(attemptRepository.findById(50L)).thenReturn(Optional.of(a));

        AttemptResult res = service.submit(50L, new SubmitRequest(List.of(new AnswerRequest(10L, List.of(100L)))), me);

        assertThat(res.summary().timedOut()).isTrue();
        assertThat(res.summary().submittedAt()).isEqualTo(a.getExpiresAt());
    }

    @Test
    void submit_someoneElsesAttempt_forbidden() {
        when(attemptRepository.findById(50L)).thenReturn(Optional.of(attempt(NOW)));
        UserPrincipal other = new UserPrincipal(6L, "o@mail.com", "x", "PARTICIPANT");

        assertThatThrownBy(() -> service.submit(50L, new SubmitRequest(List.of()), other))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void submit_twice_rejected() {
        QuizAttempt a = attempt(NOW);
        a.setStatus(AttemptStatus.SUBMITTED);
        when(attemptRepository.findById(50L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.submit(50L, new SubmitRequest(List.of()), me))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void myResults_mapsSubmittedAttempts() {
        QuizAttempt a = attempt(NOW);
        a.setStatus(AttemptStatus.SUBMITTED);
        a.setScore(2);
        when(attemptRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(5L, AttemptStatus.SUBMITTED)).thenReturn(List.of(a));

        assertThat(service.myResults(me)).singleElement().satisfies(s -> {
            assertThat(s.quizTitle()).isEqualTo("Java Basics");
            assertThat(s.percentage()).isEqualTo(100.0);
        });
    }
}
