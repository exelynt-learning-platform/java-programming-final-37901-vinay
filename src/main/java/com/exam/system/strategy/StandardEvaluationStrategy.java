package com.exam.system.strategy;

import com.exam.system.model.Exam;
import com.exam.system.model.ExamResult;
import com.exam.system.model.Question;

import java.util.List;
import java.util.Map;

/**
 * Standard Evaluation Strategy:
 * - Full points awarded for each correct answer.
 * - Zero points awarded for wrong or skipped answers.
 */
public class StandardEvaluationStrategy implements EvaluationStrategy {

    @Override
    public ExamResult evaluate(Exam exam, int studentId, List<Question> questions, Map<Integer, String> studentAnswers) {
        double totalEarned = 0.0;
        double maxPossible = 0.0;

        for (Question q : questions) {
            maxPossible += q.getPoints();
            String studentAnswer = studentAnswers != null ? studentAnswers.get(q.getId()) : null;

            if (studentAnswer != null && q.evaluateAnswer(studentAnswer)) {
                totalEarned += q.getPoints();
            }
        }

        // Avoid division by zero
        double percentage = (maxPossible > 0) ? (totalEarned / maxPossible) * 100.0 : 0.0;
        String grade = calculateGrade(percentage);
        String status = (percentage >= exam.getPassingPercentage()) ? "PASSED" : "FAILED";

        return new ExamResult(exam.getId(), studentId, totalEarned, maxPossible, percentage, grade, status);
    }

    private String calculateGrade(double percentage) {
        if (percentage >= 90.0) return "A+";
        if (percentage >= 80.0) return "A";
        if (percentage >= 70.0) return "B";
        if (percentage >= 60.0) return "C";
        if (percentage >= 50.0) return "D";
        return "F";
    }

    @Override
    public String getStrategyName() {
        return "Standard (No Penalty)";
    }
}
