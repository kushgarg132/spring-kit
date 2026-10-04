package io.github.kushgarg132.kit.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>} and, if valid, sets the authentication built by
 * {@code toAuthentication}. Never rejects a request itself: the app's SecurityFilterChain decides
 * which paths need auth, so public endpoints work without a token. Add it with
 * {@code http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)}.
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtCodec jwt;
    private final Function<Claims, Optional<Authentication>> toAuthentication;

    public BearerTokenFilter(JwtCodec jwt, Function<Claims, Optional<Authentication>> toAuthentication) {
        this.jwt = jwt;
        this.toAuthentication = toAuthentication;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional.ofNullable(request.getHeader("Authorization"))
                .filter(h -> h.startsWith(BEARER_PREFIX))
                .map(h -> h.substring(BEARER_PREFIX.length()))
                .flatMap(jwt::parse)
                .flatMap(toAuthentication)
                .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
        chain.doFilter(request, response);
    }
}
