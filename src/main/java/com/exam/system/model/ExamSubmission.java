package com.exam.system.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain model representing a student's answer submission for an exam.
 */
public class ExamSubmission {

    private final int examId;
    private final int studentId;
    private final Map<Integer, String> studentAnswers = new HashMap<>();
    private final LocalDateTime submittedAt;
    private final boolean autoSubmitted;

    public ExamSubmission(int examId, int studentId, Map<Integer, String> answers, boolean autoSubmitted) {
        this.examId = examId;
        this.studentId = studentId;
        if (answers != null) {
            this.studentAnswers.putAll(answers);
        }
        this.submittedAt = LocalDateTime.now();
        this.autoSubmitted = autoSubmitted;
    }

    public int getExamId() {
        return examId;
    }

    public int getStudentId() {
        return studentId;
    }

    public Map<Integer, String> getStudentAnswers() {
        return Collections.unmodifiableMap(studentAnswers);
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public boolean isAutoSubmitted() {
        return autoSubmitted;
    }
}
