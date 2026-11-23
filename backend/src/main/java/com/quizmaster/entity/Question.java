package com.quizmaster.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String text;

    private int position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<AnswerOption> options = new ArrayList<>();

    public void addOption(AnswerOption o) {
        o.setQuestion(this);
        options.add(o);
    }

    public Set<Long> correctOptionIds() {
        return options.stream().filter(AnswerOption::isCorrect).map(AnswerOption::getId).collect(Collectors.toSet());
    }

    /** True when more than one option is correct (UI shows checkboxes instead of radio buttons). */
    public boolean isMultipleCorrect() {
        return options.stream().filter(AnswerOption::isCorrect).count() > 1;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    public List<AnswerOption> getOptions() { return options; }
}
