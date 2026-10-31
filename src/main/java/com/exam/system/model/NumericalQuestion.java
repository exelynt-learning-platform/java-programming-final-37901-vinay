package com.exam.system.model;

import java.util.Collections;
import java.util.List;

/**
 * Question requiring numerical calculation, with tolerance for floating-point values.
 */
public class NumericalQuestion extends Question {

    private final double tolerance;

    public NumericalQuestion(int id, String text, String subject, Difficulty difficulty,
                             String correctAnswer, double points, double tolerance) {
        super(id, text, subject, difficulty, QuestionType.NUMERICAL, correctAnswer, points);
        this.tolerance = tolerance;
    }

    public NumericalQuestion(int id, String text, String subject, Difficulty difficulty,
                             String correctAnswer, double points) {
        this(id, text, subject, difficulty, correctAnswer, points, 0.01);
    }

    @Override
    public boolean evaluateAnswer(String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return false;
        }
        try {
            double expected = Double.parseDouble(correctAnswer.trim());
            double actual = Double.parseDouble(studentAnswer.trim());
            return Math.abs(expected - actual) <= tolerance;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> getOptions() {
        return Collections.emptyList();
    }
}
