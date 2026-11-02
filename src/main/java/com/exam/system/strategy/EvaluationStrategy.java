package com.exam.system.strategy;

import com.exam.system.model.Exam;
import com.exam.system.model.ExamResult;
import com.exam.system.model.Question;

import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 *             DESIGN PATTERN: STRATEGY (Evaluation Logic)
 * =============================================================================
 * Abstraction enabling interchangeable scoring algorithms (Standard, Negative Marking, etc.).
 */
public interface EvaluationStrategy {

    /**
     * Evaluates a set of answers against question definitions and generates an ExamResult.
     */
    ExamResult evaluate(Exam exam, int studentId, List<Question> questions, Map<Integer, String> studentAnswers);

    String getStrategyName();
}
