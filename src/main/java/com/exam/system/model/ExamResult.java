package com.exam.system.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Domain model representing the computed evaluation result for an exam attempt.
 */
public class ExamResult {

    private int id;
    private final int examId;
    private final int studentId;
    private String studentName;
    private String examTitle;
    private final double score;
    private final double maxScore;
    private final double percentage;
    private final String grade;
    private final String status; // PASSED or FAILED
    private final String evaluatedAt;

    public ExamResult(int id, int examId, int studentId, double score, double maxScore,
                      double percentage, String grade, String status, String evaluatedAt) {
        this.id = id;
        this.examId = examId;
        this.studentId = studentId;
        this.score = score;
        this.maxScore = maxScore;
        this.percentage = percentage;
        this.grade = grade;
        this.status = status;
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public ExamResult(int examId, int studentId, double score, double maxScore,
                      double percentage, String grade, String status) {
        this(0, examId, studentId, score, maxScore, percentage, grade, status, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExamId() {
        return examId;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public double getScore() {
        return score;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getGrade() {
        return grade;
    }

    public String getStatus() {
        return status;
    }

    public String getEvaluatedAt() {
        return evaluatedAt;
    }

    @Override
    public String toString() {
        return String.format("Result[ID=%d | Student=%d | Exam=%d | Score=%.1f/%.1f (%.1f%%) | Grade=%s | Status=%s]",
                id, studentId, examId, score, maxScore, percentage, grade, status);
    }
}
