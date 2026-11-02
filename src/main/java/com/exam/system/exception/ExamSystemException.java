package com.exam.system.exception;

/**
 * Base unchecked exception for all exam system business logic errors.
 */
public class ExamSystemException extends RuntimeException {
    public ExamSystemException(String message) {
        super(message);
    }

    public ExamSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
