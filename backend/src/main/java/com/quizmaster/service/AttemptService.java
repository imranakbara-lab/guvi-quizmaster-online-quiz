package com.quizmaster.service;

import com.quizmaster.dto.*;
import com.quizmaster.entity.*;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.exception.ForbiddenException;
import com.quizmaster.exception.ResourceNotFoundException;
import com.quizmaster.repository.QuizAttemptRepository;
import com.quizmaster.repository.QuizRepository;
import com.quizmaster.repository.UserRepository;
import com.quizmaster.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Taking a quiz: start with a timer, submit answers, automatic grading and result history. */
@Service
public class AttemptService {

    /** Extra seconds allowed for network delay when the timer hits zero. */
    static final int GRACE_SECONDS = 30;

    private final QuizAttemptRepository attemptRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final Clock clock;

    public AttemptService(QuizAttemptRepository attemptRepository, QuizRepository quizRepository,
                          UserRepository userRepository, EmailService emailService, Clock clock) {
        this.attemptRepository = attemptRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.clock = clock;
    }

    /** Starts a new attempt, or resumes the running one if its time has not run out. */
    @Transactional
    public AttemptStartResponse start(Long quizId, UserPrincipal me) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id " + quizId));
        if (!quiz.isPublished() || quiz.getQuestions().isEmpty()) {
            throw new BadRequestException("This quiz is not available");
        }
        LocalDateTime now = now();

        Optional<QuizAttempt> running = attemptRepository
                .findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(me.getId(), quizId, AttemptStatus.IN_PROGRESS);
        if (running.isPresent()) {
            QuizAttempt a = running.get();
            if (now.isBefore(a.getExpiresAt())) return toStart(a, now);
            grade(a, List.of(), now);          // time over: close it with no answers
        }

        User user = userRepository.findById(me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        QuizAttempt a = new QuizAttempt();
        a.setUser(user);
        a.setQuiz(quiz);
        a.setStartedAt(now);
        a.setExpiresAt(now.plusMinutes(quiz.getTimeLimitMinutes()));
        a.setTotalQuestions(quiz.getQuestions().size());
        return toStart(attemptRepository.save(a), now);
    }

    @Transactional
    public AttemptResult submit(Long attemptId, SubmitRequest req, UserPrincipal me) {
        QuizAttempt a = findAttempt(attemptId);
        if (!a.getUser().getId().equals(me.getId())) throw new ForbiddenException("This is not your attempt");
        if (a.getStatus() == AttemptStatus.SUBMITTED) throw new BadRequestException("This attempt was already submitted");
        grade(a, req == null || req.answers() == null ? List.of() : req.answers(), now());
        User u = a.getUser();
        emailService.sendResult(u.getEmail(), u.getName(), a.getQuiz().getTitle(), a.getScore(), a.getTotalQuestions(),
                a.getPercentage(), a.isTimedOut(), a.getId());
        return toResult(a);
    }

    @Transactional(readOnly = true)
    public AttemptResult getResult(Long attemptId, UserPrincipal me) {
        QuizAttempt a = findAttempt(attemptId);
        if (!me.isAdmin() && !a.getUser().getId().equals(me.getId())) throw new ForbiddenException("This is not your attempt");
        if (a.getStatus() != AttemptStatus.SUBMITTED) throw new BadRequestException("This attempt is still in progress");
        return toResult(a);
    }

    @Transactional(readOnly = true)
    public List<AttemptSummary> myResults(UserPrincipal me) {
        return attemptRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(me.getId(), AttemptStatus.SUBMITTED)
                .stream().map(AttemptSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AttemptSummary> allResults(Long quizId) {
        List<QuizAttempt> list = quizId == null
                ? attemptRepository.findByStatusOrderBySubmittedAtDesc(AttemptStatus.SUBMITTED)
                : attemptRepository.findByQuizIdAndStatusOrderByScoreDescSubmittedAtAsc(quizId, AttemptStatus.SUBMITTED);
        return list.stream().map(AttemptSummary::from).toList();
    }

    // ---------- grading ----------

    /**
     * A question counts as correct only when the selected options are exactly the correct options.
     * Unknown question / option ids are ignored.
     */
    void grade(QuizAttempt a, List<AnswerRequest> answers, LocalDateTime now) {
        Map<Long, Set<Long>> selectedByQuestion = new HashMap<>();
        for (AnswerRequest ans : answers) {
            if (ans == null || ans.questionId() == null) continue;
            Set<Long> ids = ans.selectedOptionIds() == null ? Set.of()
                    : ans.selectedOptionIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            selectedByQuestion.put(ans.questionId(), ids);
        }

        a.getAnswers().clear();
        int score = 0;
        for (Question q : a.getQuiz().getQuestions()) {
            Set<Long> valid = q.getOptions().stream().map(AnswerOption::getId).collect(Collectors.toSet());
            Set<Long> picked = new HashSet<>(selectedByQuestion.getOrDefault(q.getId(), Set.of()));
            picked.retainAll(valid);
            boolean correct = !picked.isEmpty() && picked.equals(q.correctOptionIds());
            if (correct) score++;
            a.addAnswer(new AttemptAnswer(q.getId(), picked, correct));
        }
        a.setScore(score);
        a.setTotalQuestions(a.getQuiz().getQuestions().size());
        a.setTimedOut(now.isAfter(a.getExpiresAt()));
        a.setSubmittedAt(now.isAfter(a.getExpiresAt().plusSeconds(GRACE_SECONDS)) ? a.getExpiresAt() : now);
        a.setStatus(AttemptStatus.SUBMITTED);
        attemptRepository.save(a);
    }

    // ---------- mapping ----------

    private AttemptStartResponse toStart(QuizAttempt a, LocalDateTime now) {
        Quiz quiz = a.getQuiz();
        long remaining = Math.max(0, Duration.between(now, a.getExpiresAt()).getSeconds());
        return new AttemptStartResponse(a.getId(), quiz.getId(), quiz.getTitle(), quiz.getDescription(),
                quiz.getTimeLimitMinutes(), remaining, quiz.getQuestions().stream().map(QuestionView::from).toList());
    }

    private AttemptResult toResult(QuizAttempt a) {
        Map<Long, AttemptAnswer> byQuestion = a.getAnswers().stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity(), (x, y) -> x));
        List<ReviewItem> review = new ArrayList<>();
        for (Question q : a.getQuiz().getQuestions()) {
            AttemptAnswer ans = byQuestion.get(q.getId());
            if (ans == null) continue;     // question added after this attempt
            review.add(new ReviewItem(q.getId(), q.getText(),
                    q.getOptions().stream().map(OptionAdminView::from).toList(),
                    new ArrayList<>(ans.selectedIds()), ans.isCorrect()));
        }
        return new AttemptResult(AttemptSummary.from(a), review);
    }

    private QuizAttempt findAttempt(Long id) {
        return attemptRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id " + id));
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }
}
