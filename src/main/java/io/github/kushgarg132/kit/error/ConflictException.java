package io.github.kushgarg132.kit.error;

import java.util.Map;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(String message, Map<String, String> details) {
        super(ErrorCode.CONFLICT, message, details);
    }
}
