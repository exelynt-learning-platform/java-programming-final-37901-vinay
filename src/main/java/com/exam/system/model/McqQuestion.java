package com.exam.system.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multiple Choice Question (MCQ) with 4 discrete options.
 */
public class McqQuestion extends Question {

    private final List<String> options;

    public McqQuestion(int id, String text, String subject, Difficulty difficulty,
                       List<String> options, String correctAnswer, double points) {
        super(id, text, subject, difficulty, QuestionType.MCQ, correctAnswer, points);
        this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
    }

    @Override
    public boolean evaluateAnswer(String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return false;
        }
        return correctAnswer.trim().equalsIgnoreCase(studentAnswer.trim());
    }

    @Override
    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }
}
