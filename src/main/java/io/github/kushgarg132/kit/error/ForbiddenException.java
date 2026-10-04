package io.github.kushgarg132.kit.error;

import java.util.Map;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(String message, Map<String, String> details) {
        super(ErrorCode.FORBIDDEN, message, details);
    }
}
