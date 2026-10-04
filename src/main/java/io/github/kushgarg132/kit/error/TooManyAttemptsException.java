package io.github.kushgarg132.kit.error;

import java.util.Map;

public class TooManyAttemptsException extends ApiException {

    public TooManyAttemptsException(String message) {
        super(ErrorCode.RATE_LIMITED, message);
    }

    public TooManyAttemptsException(String message, Map<String, String> details) {
        super(ErrorCode.RATE_LIMITED, message, details);
    }
}
