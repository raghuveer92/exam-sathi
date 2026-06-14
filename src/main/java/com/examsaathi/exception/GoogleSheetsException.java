package com.examsaathi.exception;

import lombok.Getter;

/**
 * Google Sheets integration errors with stable codes for clients.
 */
@Getter
public class GoogleSheetsException extends RuntimeException {

    public enum ErrorCode {
        SHEET_NOT_CONFIGURED,
        SHEET_ACCESS_FAILED,
        SHEET_EMPTY,
        SHEET_INVALID_FORMAT,
        TOPIC_EMPTY,
        INSUFFICIENT_QUESTIONS,
        CACHE_FAILURE
    }

    private final ErrorCode errorCode;

    public GoogleSheetsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GoogleSheetsException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
