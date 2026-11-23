package com.quizmaster.entity;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** The options a participant picked for one question, and whether that was right. */
@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    /** Kept as a plain id so deleting a question later does not break old results. */
    @Column(nullable = false)
    private Long questionId;

    /** Selected option ids, comma separated (e.g. "4,7"). Empty when skipped. */
    @Column(length = 500)
    private String selectedOptionIds = "";

    private boolean correct;

    public AttemptAnswer() {}

    public AttemptAnswer(Long questionId, Collection<Long> selected, boolean correct) {
        this.questionId = questionId;
        this.selectedOptionIds = selected == null ? "" :
                new TreeSet<>(selected).stream().map(String::valueOf).collect(Collectors.joining(","));
        this.correct = correct;
    }

    public Set<Long> selectedIds() {
        if (selectedOptionIds == null || selectedOptionIds.isBlank()) return Set.of();
        return Arrays.stream(selectedOptionIds.split(",")).map(Long::valueOf).collect(Collectors.toCollection(TreeSet::new));
    }

    public Long getId() { return id; }
    public QuizAttempt getAttempt() { return attempt; }
    public void setAttempt(QuizAttempt attempt) { this.attempt = attempt; }
    public Long getQuestionId() { return questionId; }
    public String getSelectedOptionIds() { return selectedOptionIds; }
    public boolean isCorrect() { return correct; }
}
