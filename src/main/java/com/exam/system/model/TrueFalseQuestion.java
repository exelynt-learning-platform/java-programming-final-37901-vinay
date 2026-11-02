package com.exam.system.model;

import java.util.Arrays;
import java.util.List;

/**
 * Binary True/False question.
 */
public class TrueFalseQuestion extends Question {

    public TrueFalseQuestion(int id, String text, String subject, Difficulty difficulty,
                             String correctAnswer, double points) {
        super(id, text, subject, difficulty, QuestionType.TRUE_FALSE, correctAnswer, points);
    }

    @Override
    public boolean evaluateAnswer(String studentAnswer) {
        if (studentAnswer == null) return false;
        String normalizedAnswer = studentAnswer.trim().toLowerCase();
        String normalizedCorrect = correctAnswer.trim().toLowerCase();

        boolean ansBool = normalizedAnswer.equals("true") || normalizedAnswer.equals("t") || normalizedAnswer.equals("1");
        boolean correctBool = normalizedCorrect.equals("true") || normalizedCorrect.equals("t") || normalizedCorrect.equals("1");

        return ansBool == correctBool;
    }

    @Override
    public List<String> getOptions() {
        return Arrays.asList("True", "False");
    }
}
