package com.exam.system.exception;

/**
 * Thrown when an exam configuration, question assignment, or submission state is invalid.
 */
public class InvalidExamException extends ExamSystemException {
    public InvalidExamException(String message) {
        super(message);
    }
}
