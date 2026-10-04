package io.github.kushgarg132.kit.web;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, String message, Instant timestamp) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(true, data, "OK", Instant.now());
    }

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now());
    }
}
