package com.examsaathi.exception;

/**
 * Thrown when Brevo email delivery fails after retries.
 * Marked retryable at the service layer for transient API failures.
 */
public class EmailDeliveryException extends RuntimeException {

    private final boolean retryable;

    public EmailDeliveryException(String message) {
        this(message, false);
    }

    public EmailDeliveryException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public EmailDeliveryException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
