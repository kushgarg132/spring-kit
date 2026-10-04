package io.github.kushgarg132.kit.error;

import io.github.kushgarg132.kit.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception into the {@link ApiErrorResponse} envelope. Registered by
 * {@code KitAutoConfiguration} at lowest precedence, so an app's own {@code @RestControllerAdvice}
 * handlers win for anything they cover.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return respond(ex.getErrorCode(), ex.getMessage(), ex.getDetails(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> details.put(
                fieldError.getField(),
                fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage()));
        return respond(ErrorCode.VALIDATION_FAILED, "Validation failed", details, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return respond(ErrorCode.UNAUTHORIZED, "Authentication required", Map.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(ErrorCode.FORBIDDEN, "Access denied", Map.of(), request);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(Exception ex, HttpServletRequest request) {
        log.warn("Write conflict on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return respond(ErrorCode.CONFLICT, "That change conflicts with another update. Try again.", Map.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        return respond(ErrorCode.RESOURCE_NOT_FOUND, "Not found", Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", Map.of(), request);
    }

    private ResponseEntity<ApiErrorResponse> respond(
            ErrorCode errorCode, String message, Map<String, String> details, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiErrorResponse.of(message, errorCode.name(), details, request.getRequestURI()));
    }
}
