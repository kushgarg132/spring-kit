package io.github.kushgarg132.kit.error;

import java.util.Map;

/** Base type for every deliberately-thrown exception; {@link GlobalExceptionHandler} maps it to its status. */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, String> details;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public ApiException(ErrorCode errorCode, String message, Map<String, String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
