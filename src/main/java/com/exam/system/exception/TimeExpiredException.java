package com.exam.system.exception;

/**
 * Thrown when an exam duration has elapsed and auto-submission is invoked.
 */
public class TimeExpiredException extends ExamSystemException {
    public TimeExpiredException(String message) {
        super(message);
    }
}
