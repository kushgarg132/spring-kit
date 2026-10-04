package io.github.kushgarg132.kit.error;

import java.util.Map;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    public BadRequestException(String message, Map<String, String> details) {
        super(ErrorCode.BAD_REQUEST, message, details);
    }
}
