package com.exam.system.strategy;

import com.exam.system.model.Exam;
import com.exam.system.model.ExamResult;
import com.exam.system.model.Question;

import java.util.List;
import java.util.Map;

/**
 * Penalty Evaluation Strategy (Negative Marking):
 * - Correct answer: +100% points.
 * - Incorrect answer: -25% penalty deduction.
 * - Skipped/Unanswered: 0 penalty.
 */
public class PenaltyEvaluationStrategy implements EvaluationStrategy {

    private final double penaltyFraction;

    public PenaltyEvaluationStrategy(double penaltyFraction) {
        this.penaltyFraction = penaltyFraction;
    }

    public PenaltyEvaluationStrategy() {
        this(0.25);
    }

    @Override
    public ExamResult evaluate(Exam exam, int studentId, List<Question> questions, Map<Integer, String> studentAnswers) {
        double totalEarned = 0.0;
        double maxPossible = 0.0;

        for (Question q : questions) {
            maxPossible += q.getPoints();
            String studentAnswer = studentAnswers != null ? studentAnswers.get(q.getId()) : null;

            if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
                // Skipped: No penalty
                continue;
            }

            if (q.evaluateAnswer(studentAnswer)) {
                totalEarned += q.getPoints();
            } else {
                totalEarned -= (q.getPoints() * penaltyFraction);
            }
        }

        // Clamp minimum score at 0.0
        totalEarned = Math.max(0.0, totalEarned);

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
        return "Negative Marking (-" + (int)(penaltyFraction * 100) + "% Penalty)";
    }
}
