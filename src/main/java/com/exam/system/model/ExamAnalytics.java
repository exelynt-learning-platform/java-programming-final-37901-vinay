package com.exam.system.model;

/**
 * Model summarizing statistical analytics for an exam.
 */
public class ExamAnalytics {

    private final int examId;
    private final String examTitle;
    private final int totalAttempts;
    private final int passedCount;
    private final int failedCount;
    private final double passRatePercentage;
    private final double averageScore;
    private final double highestScore;
    private final double lowestScore;

    public ExamAnalytics(int examId, String examTitle, int totalAttempts, int passedCount, int failedCount,
                         double passRatePercentage, double averageScore, double highestScore, double lowestScore) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.totalAttempts = totalAttempts;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.passRatePercentage = passRatePercentage;
        this.averageScore = averageScore;
        this.highestScore = highestScore;
        this.lowestScore = lowestScore;
    }

    public int getExamId() {
        return examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public double getPassRatePercentage() {
        return passRatePercentage;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public double getHighestScore() {
        return highestScore;
    }

    public double getLowestScore() {
        return lowestScore;
    }

    @Override
    public String toString() {
        return String.format("Analytics[Exam=%s | Attempts=%d | PassRate=%.1f%% | Avg=%.2f | High=%.1f | Low=%.1f]",
                examTitle, totalAttempts, passRatePercentage, averageScore, highestScore, lowestScore);
    }
}
