package io.github.hectorvent.floci.core.common;

/**
 * Base exception for AWS emulator errors.
 * Maps to AWS-style error responses with code, message, and HTTP status.
 */
public class AwsException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public AwsException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
