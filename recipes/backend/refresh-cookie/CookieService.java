package recipes.refreshcookie; // snapshot: adapt package + imports

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.Optional;

@Service
public class CookieService {

    public static final String REFRESH_COOKIE = "refresh_token";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private static final String AUTH_PATH = "/api/v1/auth";

    private final boolean secure;
    private final String sameSite;

    public CookieService(
            @Value("${app.auth.cookie-secure}") boolean secure,
            @Value("${app.auth.cookie-same-site}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie refreshTokenCookie(String rawToken, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(AUTH_PATH)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie expiredRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(AUTH_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    /** Readable by JS (double-submit CSRF defense for the two cookie-authenticated endpoints). */
    public ResponseCookie csrfCookie(String token, Duration maxAge) {
        return ResponseCookie.from(CSRF_COOKIE, token)
                .httpOnly(false)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(WebUtils.getCookie(request, REFRESH_COOKIE)).map(jakarta.servlet.http.Cookie::getValue);
    }

    public boolean isCsrfValid(HttpServletRequest request) {
        String cookieValue = Optional.ofNullable(WebUtils.getCookie(request, CSRF_COOKIE))
                .map(jakarta.servlet.http.Cookie::getValue)
                .orElse(null);
        String headerValue = request.getHeader(CSRF_HEADER);
        return cookieValue != null && cookieValue.equals(headerValue);
    }
}
