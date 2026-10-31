package com.exam.system.factory;

import com.exam.system.model.Difficulty;
import com.exam.system.model.McqQuestion;
import com.exam.system.model.NumericalQuestion;
import com.exam.system.model.Question;
import com.exam.system.model.QuestionType;
import com.exam.system.model.TrueFalseQuestion;

import java.util.List;

/**
 * =============================================================================
 *             DESIGN PATTERN: FACTORY (Question Type Creation)
 * =============================================================================
 * Centralized object creation factory for polymorphic question entities.
 * Clients decouple from concrete implementations (McqQuestion, TrueFalseQuestion, NumericalQuestion).
 */
public class QuestionFactory {

    public static Question createQuestion(int id, String text, String subject, Difficulty difficulty,
                                          QuestionType type, List<String> options, String correctAnswer,
                                          double points) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text cannot be empty.");
        }
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            throw new IllegalArgumentException("Correct answer must be provided.");
        }

        switch (type) {
            case MCQ:
                if (options == null || options.size() < 2) {
                    throw new IllegalArgumentException("MCQ requires at least 2 answer choices.");
                }
                return new McqQuestion(id, text, subject, difficulty, options, correctAnswer, points);

            case TRUE_FALSE:
                return new TrueFalseQuestion(id, text, subject, difficulty, correctAnswer, points);

            case NUMERICAL:
                return new NumericalQuestion(id, text, subject, difficulty, correctAnswer, points);

            default:
                throw new IllegalArgumentException("Unsupported question type: " + type);
        }
    }
}
