package io.github.kushgarg132.kit.web;

import java.time.Instant;
import java.util.Map;

/** Uniform error envelope, so a frontend never branches on error shape. */
public record ApiErrorResponse(
        boolean success, String message, String errorCode, Map<String, String> details, Instant timestamp, String path) {

    public static ApiErrorResponse of(String message, String errorCode, Map<String, String> details, String path) {
        return new ApiErrorResponse(false, message, errorCode, details, Instant.now(), path);
    }
}
