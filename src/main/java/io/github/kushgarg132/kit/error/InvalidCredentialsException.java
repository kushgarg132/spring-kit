package io.github.kushgarg132.kit.error;

import java.util.Map;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public InvalidCredentialsException(String message, Map<String, String> details) {
        super(ErrorCode.UNAUTHORIZED, message, details);
    }
}
