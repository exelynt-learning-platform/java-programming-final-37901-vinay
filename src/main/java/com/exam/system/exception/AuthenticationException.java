package com.exam.system.exception;

/**
 * Thrown when user credentials, login tokens, or role authorizations are invalid.
 */
public class AuthenticationException extends ExamSystemException {
    public AuthenticationException(String message) {
        super(message);
    }
}
