package com.exam.system.model;

import java.util.List;

/**
 * =============================================================================
 *                       ABSTRACT QUESTION TEMPLATE
 * =============================================================================
 * Base class defining the common abstraction for all exam question types.
 */
public abstract class Question {

    protected int id;
    protected String text;
    protected String subject;
    protected Difficulty difficulty;
    protected QuestionType questionType;
    protected String correctAnswer;
    protected double points;

    public Question(int id, String text, String subject, Difficulty difficulty,
                    QuestionType questionType, String correctAnswer, double points) {
        this.id = id;
        this.text = text;
        this.subject = subject;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.correctAnswer = correctAnswer;
        this.points = points;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

    /**
     * Polymorphic evaluation method.
     */
    public abstract boolean evaluateAnswer(String studentAnswer);

    /**
     * Returns options for display (empty list for non-choice questions).
     */
    public abstract List<String> getOptions();

    @Override
    public String toString() {
        return String.format("[%s | %s | %s | %.1f pts] %s",
                subject, difficulty, questionType, points, text);
    }
}
