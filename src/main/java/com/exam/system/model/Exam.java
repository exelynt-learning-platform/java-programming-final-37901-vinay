package com.exam.system.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain model representing an Exam definition with questions and schedule.
 */
public class Exam {

    private int id;
    private final String title;
    private final String subject;
    private final int durationMinutes;
    private final String scheduledTime;
    private final List<Integer> questionIds = new ArrayList<>();
    private double totalMarks;
    private double passingPercentage;

    public Exam(int id, String title, String subject, int durationMinutes,
                String scheduledTime, double totalMarks, double passingPercentage) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.durationMinutes = durationMinutes;
        this.scheduledTime = scheduledTime;
        this.totalMarks = totalMarks;
        this.passingPercentage = passingPercentage;
    }

    public Exam(String title, String subject, int durationMinutes,
                String scheduledTime, double totalMarks, double passingPercentage) {
        this(0, title, subject, durationMinutes, scheduledTime, totalMarks, passingPercentage);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public List<Integer> getQuestionIds() {
        return Collections.unmodifiableList(questionIds);
    }

    public void addQuestionId(int questionId) {
        if (!questionIds.contains(questionId)) {
            questionIds.add(questionId);
        }
    }

    public void setQuestionIds(List<Integer> ids) {
        questionIds.clear();
        if (ids != null) {
            questionIds.addAll(ids);
        }
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getPassingPercentage() {
        return passingPercentage;
    }

    public void setPassingPercentage(double passingPercentage) {
        this.passingPercentage = passingPercentage;
    }

    @Override
    public String toString() {
        return String.format("Exam[ID=%d | '%s' | %s | %d mins | %d Questions | %.1f Marks]",
                id, title, subject, durationMinutes, questionIds.size(), totalMarks);
    }
}
