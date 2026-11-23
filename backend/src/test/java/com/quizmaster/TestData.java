package com.quizmaster;

import com.quizmaster.entity.*;

/** Helpers to build entities with ids for unit tests. */
public final class TestData {

    private TestData() {}

    public static User user(long id, String email, Role role) {
        User u = new User("User " + id, email, "ENC", null, role);
        u.setId(id);
        return u;
    }

    /**
     * Quiz 1 with two questions:
     *  Q10: options 100 (correct), 101
     *  Q20: options 200 (correct), 201 (correct), 202   - multiple correct
     */
    public static Quiz quiz() {
        Quiz quiz = new Quiz();
        quiz.setId(1L);
        quiz.setTitle("Java Basics");
        quiz.setDescription("desc");
        quiz.setTimeLimitMinutes(10);
        quiz.setPublished(true);
        quiz.addQuestion(question(10L, "Q1", new long[]{100, 101}, 100));
        quiz.addQuestion(question(20L, "Q2", new long[]{200, 201, 202}, 200, 201));
        return quiz;
    }

    public static Question question(long id, String text, long[] optionIds, long... correctIds) {
        Question q = new Question();
        q.setId(id);
        q.setText(text);
        for (long oid : optionIds) {
            boolean correct = java.util.Arrays.stream(correctIds).anyMatch(c -> c == oid);
            AnswerOption o = new AnswerOption("Option " + oid, correct);
            o.setId(oid);
            q.addOption(o);
        }
        return q;
    }
}
