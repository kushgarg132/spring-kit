package io.github.kushgarg132.kit.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Real client IP behind Nginx. Nginx overwrites {@code X-Real-IP} with the peer address and the
 * backend only listens on 127.0.0.1, so clients can't forge it. {@code X-Forwarded-For}'s first
 * entry is client-supplied and is deliberately not used.
 */
public final class ClientIp {

    private ClientIp() {}

    public static String of(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }

    /** IP of the request on this thread; null outside a request (e.g. a scheduled job). */
    public static String current() {
        return currentRequest().map(ClientIp::of).orElse(null);
    }

    public static String currentUserAgent() {
        return currentRequest().map(r -> r.getHeader("User-Agent")).orElse(null);
    }

    private static Optional<HttpServletRequest> currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? Optional.of(attrs.getRequest())
                : Optional.empty();
    }
}
