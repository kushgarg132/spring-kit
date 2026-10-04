package recipes.stompauth; // snapshot: adapt package + imports

import app.domain.admin.Admin;
import app.domain.admin.AdminRepository;
import app.infrastructure.security.AuthenticatedPrincipal;
import app.infrastructure.security.JwtService;
import app.infrastructure.security.WardAccessGuard;
import io.jsonwebtoken.JwtException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticates the STOMP CONNECT frame (browsers can't set custom headers on the raw
 * WebSocket upgrade request, but a STOMP CONNECT frame sent after the socket opens can
 * carry an Authorization header) and authorizes per-ward SUBSCRIBE frames against the
 * same WardAccessGuard.canView the equivalent REST read already uses.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern WARD_TOPIC = Pattern.compile("^/topic/wards/([0-9a-fA-F-]{36})$");
    private static final Pattern USER_ACCESS_TOPIC = Pattern.compile("^/topic/users/([0-9a-fA-F-]{36})/access$");

    private final JwtService jwtService;
    private final AdminRepository adminRepository;
    private final WardAccessGuard wardAccessGuard;

    public StompAuthChannelInterceptor(JwtService jwtService, AdminRepository adminRepository,
                                        WardAccessGuard wardAccessGuard) {
        this.jwtService = jwtService;
        this.adminRepository = adminRepository;
        this.wardAccessGuard = wardAccessGuard;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }
        return message;
    }

    private AuthenticatedPrincipal authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing bearer token");
        }
        UUID adminId;
        try {
            var claims = jwtService.parse(header.substring(7));
            adminId = jwtService.getAdminId(claims);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid token", e);
        }
        // Same enforcement as JwtAuthenticationFilter over HTTP — a deactivated admin's
        // token must not be honored here either.
        boolean active = adminRepository.findById(adminId).map(Admin::isActive).orElse(false);
        if (!active) {
            throw new BadCredentialsException("Account is deactivated");
        }
        return new AuthenticatedPrincipal(adminId);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher wardMatcher = WARD_TOPIC.matcher(destination);
        if (wardMatcher.matches()) {
            AuthenticatedPrincipal principal = requireUser(accessor);
            UUID wardId = UUID.fromString(wardMatcher.group(1));

            // WardAccessGuard reads the caller from SecurityContextHolder, which channel
            // interceptors don't get for free (they run outside the servlet filter chain) —
            // populate it just for this check, same trick JwtAuthenticationFilter does over HTTP.
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            try {
                if (!wardAccessGuard.canView(wardId)) {
                    throw new AccessDeniedException("Not authorized for ward " + wardId);
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
            return;
        }

        Matcher userAccessMatcher = USER_ACCESS_TOPIC.matcher(destination);
        if (userAccessMatcher.matches()) {
            AuthenticatedPrincipal principal = requireUser(accessor);
            UUID topicUserId = UUID.fromString(userAccessMatcher.group(1));
            if (!principal.userId().equals(topicUserId)) {
                throw new AccessDeniedException("Cannot subscribe to another user's access topic");
            }
        }
    }

    private AuthenticatedPrincipal requireUser(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof AuthenticatedPrincipal principal)) {
            throw new AccessDeniedException("Unauthenticated subscribe");
        }
        return principal;
    }
}
