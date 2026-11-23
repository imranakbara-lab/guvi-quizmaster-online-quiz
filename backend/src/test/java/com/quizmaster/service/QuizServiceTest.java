package com.quizmaster.service;

import com.quizmaster.TestData;
import com.quizmaster.dto.*;
import com.quizmaster.entity.Question;
import com.quizmaster.entity.Quiz;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.exception.ResourceNotFoundException;
import com.quizmaster.repository.QuestionRepository;
import com.quizmaster.repository.QuizAttemptRepository;
import com.quizmaster.repository.QuizRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuizAttemptRepository attemptRepository;

    @InjectMocks private QuizService quizService;

    private static QuestionRequest question(boolean anyCorrect) {
        return new QuestionRequest("2 + 2 = ?", List.of(new OptionRequest("4", anyCorrect), new OptionRequest("5", false)));
    }

    @Test
    void create_savesDraftQuiz() {
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizDetail res = quizService.create(new QuizRequest(" Maths ", "Basic sums", 15, false));

        assertThat(res.title()).isEqualTo("Maths");
        assertThat(res.timeLimitMinutes()).isEqualTo(15);
        assertThat(res.published()).isFalse();
    }

    @Test
    void create_publishedWithoutQuestions_rejected() {
        assertThatThrownBy(() -> quizService.create(new QuizRequest("Maths", null, 15, true)))
                .isInstanceOf(BadRequestException.class);
        verify(quizRepository, never()).save(any());
    }

    @Test
    void update_canPublishQuizWithQuestions() {
        Quiz quiz = TestData.quiz();
        quiz.setPublished(false);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(quizService.update(1L, new QuizRequest("Java", "d", 20, true)).published()).isTrue();
    }

    @Test
    void addQuestion_withoutCorrectOption_rejected() {
        assertThatThrownBy(() -> quizService.addQuestion(1L, question(false)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("correct");
        verifyNoInteractions(quizRepository);
    }

    @Test
    void addQuestion_addsWithOptionsAndPosition() {
        Quiz quiz = TestData.quiz();
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        QuestionAdminView res = quizService.addQuestion(1L, question(true));

        assertThat(quiz.getQuestions()).hasSize(3);
        assertThat(res.position()).isEqualTo(3);
        assertThat(res.options()).extracting(OptionAdminView::correct).containsExactly(true, false);
        verify(quizRepository).saveAndFlush(quiz);
    }

    @Test
    void updateQuestion_replacesOptions() {
        Question q = TestData.quiz().getQuestions().get(1);
        when(questionRepository.findById(20L)).thenReturn(Optional.of(q));
        when(questionRepository.saveAndFlush(q)).thenReturn(q);

        QuestionAdminView res = quizService.updateQuestion(20L, question(true));

        assertThat(res.text()).isEqualTo("2 + 2 = ?");
        assertThat(res.options()).hasSize(2);
    }

    @Test
    void deleteLastQuestion_unpublishesQuiz() {
        Quiz quiz = TestData.quiz();
        quiz.getQuestions().remove(1);
        Question only = quiz.getQuestions().get(0);
        when(questionRepository.findById(10L)).thenReturn(Optional.of(only));

        quizService.deleteQuestion(10L);

        assertThat(quiz.getQuestions()).isEmpty();
        assertThat(quiz.isPublished()).isFalse();
    }

    @Test
    void delete_removesAttemptsToo() {
        Quiz quiz = TestData.quiz();
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        quizService.delete(1L);

        verify(attemptRepository).deleteByQuizId(1L);
        verify(quizRepository).delete(quiz);
    }

    @Test
    void getDetail_missing_throwsNotFound() {
        when(quizRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.getDetail(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void availableQuizzes_mapsSummaries() {
        when(quizRepository.findAvailable()).thenReturn(List.of(TestData.quiz()));

        assertThat(quizService.availableQuizzes()).singleElement()
                .satisfies(s -> assertThat(s.questionCount()).isEqualTo(2));
    }
}
