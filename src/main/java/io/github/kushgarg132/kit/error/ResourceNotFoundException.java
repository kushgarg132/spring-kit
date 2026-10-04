package io.github.kushgarg132.kit.error;

import java.util.Map;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String message, Map<String, String> details) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, details);
    }

    public ResourceNotFoundException(String resource, Object identifier) {
        this("%s not found: %s".formatted(resource, identifier));
    }
}
