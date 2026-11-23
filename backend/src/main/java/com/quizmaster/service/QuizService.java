package com.quizmaster.service;

import com.quizmaster.dto.*;
import com.quizmaster.entity.AnswerOption;
import com.quizmaster.entity.Question;
import com.quizmaster.entity.Quiz;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.exception.ResourceNotFoundException;
import com.quizmaster.repository.QuestionRepository;
import com.quizmaster.repository.QuizAttemptRepository;
import com.quizmaster.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quiz and question management (admin) and the list of quizzes participants can take. */
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository,
                       QuizAttemptRepository attemptRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
    }

    // ---------- Participant ----------

    @Transactional(readOnly = true)
    public List<QuizSummary> availableQuizzes() {
        return quizRepository.findAvailable().stream().map(QuizSummary::from).toList();
    }

    // ---------- Admin: quizzes ----------

    @Transactional(readOnly = true)
    public List<QuizSummary> allQuizzes() {
        return quizRepository.findAllByOrderByCreatedAtDesc().stream().map(QuizSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public QuizDetail getDetail(Long id) {
        return QuizDetail.from(findQuiz(id));
    }

    @Transactional
    public QuizDetail create(QuizRequest req) {
        Quiz quiz = new Quiz();
        apply(quiz, req);
        if (quiz.isPublished()) throw new BadRequestException("Add at least one question before publishing");
        return QuizDetail.from(quizRepository.save(quiz));
    }

    @Transactional
    public QuizDetail update(Long id, QuizRequest req) {
        Quiz quiz = findQuiz(id);
        apply(quiz, req);
        if (quiz.isPublished() && quiz.getQuestions().isEmpty()) {
            throw new BadRequestException("Add at least one question before publishing");
        }
        return QuizDetail.from(quizRepository.save(quiz));
    }

    /** Deletes the quiz, its questions and all attempts. */
    @Transactional
    public void delete(Long id) {
        Quiz quiz = findQuiz(id);
        attemptRepository.deleteByQuizId(quiz.getId());
        quizRepository.delete(quiz);
    }

    // ---------- Admin: questions ----------

    @Transactional
    public QuestionAdminView addQuestion(Long quizId, QuestionRequest req) {
        validate(req);
        Quiz quiz = findQuiz(quizId);
        Question q = new Question();
        q.setPosition(quiz.getQuestions().size() + 1);
        applyQuestion(q, req);
        quiz.addQuestion(q);
        quizRepository.saveAndFlush(quiz);
        return QuestionAdminView.from(q);
    }

    @Transactional
    public QuestionAdminView updateQuestion(Long questionId, QuestionRequest req) {
        validate(req);
        Question q = findQuestion(questionId);
        applyQuestion(q, req);
        return QuestionAdminView.from(questionRepository.saveAndFlush(q));
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question q = findQuestion(questionId);
        Quiz quiz = q.getQuiz();
        quiz.getQuestions().remove(q);
        if (quiz.getQuestions().isEmpty()) quiz.setPublished(false);
        quizRepository.save(quiz);
    }

    // ---------- helpers ----------

    Quiz findQuiz(Long id) {
        return quizRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id " + id));
    }

    private Question findQuestion(Long id) {
        return questionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Question not found with id " + id));
    }

    private void apply(Quiz quiz, QuizRequest req) {
        quiz.setTitle(req.title().trim());
        quiz.setDescription(req.description() == null ? null : req.description().trim());
        quiz.setTimeLimitMinutes(req.timeLimitMinutes());
        quiz.setPublished(req.published());
    }

    private void applyQuestion(Question q, QuestionRequest req) {
        q.setText(req.text().trim());
        q.getOptions().clear();
        req.options().forEach(o -> q.addOption(new AnswerOption(o.text().trim(), o.correct())));
    }

    private void validate(QuestionRequest req) {
        if (req.options() == null || req.options().size() < 2) throw new BadRequestException("A question needs at least 2 options");
        if (req.options().stream().noneMatch(OptionRequest::correct)) throw new BadRequestException("Mark at least one option as correct");
    }
}
