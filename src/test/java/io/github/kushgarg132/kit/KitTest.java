package io.github.kushgarg132.kit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kushgarg132.kit.crypto.AesGcmCipher;
import io.github.kushgarg132.kit.error.GlobalExceptionHandler;
import io.github.kushgarg132.kit.error.ResourceNotFoundException;
import io.github.kushgarg132.kit.error.TooManyAttemptsException;
import io.github.kushgarg132.kit.security.AttemptLimiter;
import io.github.kushgarg132.kit.security.BearerTokenFilter;
import io.github.kushgarg132.kit.security.JwtCodec;
import io.github.kushgarg132.kit.security.OpaqueTokens;
import io.github.kushgarg132.kit.web.ClientIp;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class KitTest {

    private static final String SECRET = "a-test-secret-that-is-at-least-32-bytes-long!!";

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void jwtRoundTripsRejectsTamperingAndExpiry() {
        MutableClock clock = new MutableClock();
        JwtCodec jwt = new JwtCodec(SECRET, Duration.ofMinutes(15), clock);
        String token = jwt.issue("user-1", Map.of("email", "a@b.c"));

        assertThat(jwt.parse(token)).get().satisfies(c -> {
            assertThat(c.getSubject()).isEqualTo("user-1");
            assertThat(c.get("email", String.class)).isEqualTo("a@b.c");
        });
        assertThat(new JwtCodec(SECRET + "x", Duration.ofMinutes(15), clock).parse(token)).isEmpty();
        assertThat(jwt.parse(token + "x")).isEmpty();
        clock.now = clock.now.plus(Duration.ofMinutes(16));
        assertThat(jwt.parse(token)).isEmpty();
    }

    @Test
    void jwtAcceptsTokensFromTheAppsOldProviders() {
        // How RoutineOS / voting apps signed tokens before the kit.
        String old = Jwts.builder().subject("u").expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(new JwtCodec(SECRET, Duration.ofMinutes(1)).parse(old)).isPresent();
    }

    @Test
    void jwtRejectsShortSecretAtStartup() {
        assertThatThrownBy(() -> new JwtCodec("short", Duration.ofMinutes(1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bearerFilterAuthenticatesOnlyValidTokens() throws Exception {
        JwtCodec jwt = new JwtCodec(SECRET, Duration.ofMinutes(5));
        BearerTokenFilter filter = new BearerTokenFilter(jwt,
                c -> Optional.of(new UsernamePasswordAuthenticationToken(c.getSubject(), null, List.of())));
        AtomicReference<Object> seen = new AtomicReference<>();
        for (String header : new String[] {"Bearer " + jwt.issue("u1", Map.of()), "Bearer junk", null}) {
            SecurityContextHolder.clearContext();
            MockHttpServletRequest req = new MockHttpServletRequest();
            if (header != null) req.addHeader("Authorization", header);
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (seen.get() == null) seen.set(auth == null ? "none" : auth.getPrincipal());
            else assertThat(auth).isNull();
        }
        assertThat(seen.get()).isEqualTo("u1");
        SecurityContextHolder.clearContext();
    }

    @Test
    void limiterBlocksAfterMaxFailuresAndForgetsAfterWindow() {
        MutableClock clock = new MutableClock();
        AttemptLimiter limiter = new AttemptLimiter(3, Duration.ofMinutes(15), "slow down", clock);
        for (int i = 0; i < 3; i++) {
            limiter.checkAllowed("A@x.com");
            limiter.recordFailure("a@X.com");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("a@x.com")).isInstanceOf(TooManyAttemptsException.class);
        clock.now = clock.now.plus(Duration.ofMinutes(16));
        limiter.checkAllowed("a@x.com");
        limiter.recordFailure("b");
        limiter.recordSuccess("b");
        limiter.checkAllowed("b");
    }

    @Test
    void cipherRoundTripsAndReadsValuesFromTheOldRoutineOsCipher() throws Exception {
        AesGcmCipher cipher = new AesGcmCipher("enc-secret", "test.secret");
        assertThat(cipher.decrypt(cipher.encrypt("sk-123"))).isEqualTo("sk-123");
        assertThat(cipher.encrypt("x")).isNotEqualTo(cipher.encrypt("x"));

        // Former ApiKeyCipher.encrypt, verbatim algorithm.
        byte[] iv = new byte[12];
        iv[0] = 7;
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest("enc-secret".getBytes(StandardCharsets.UTF_8)), "AES"),
                new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal("legacy".getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ct, 0, combined, iv.length, ct.length);
        assertThat(cipher.decrypt(Base64.getEncoder().encodeToString(combined))).isEqualTo("legacy");

        assertThatThrownBy(() -> new AesGcmCipher("other", "test.secret").decrypt(cipher.encrypt("x")))
                .hasMessageContaining("test.secret");
    }

    @Test
    void opaqueTokensAreRandomAndHashStably() {
        String raw = OpaqueTokens.generate();
        assertThat(raw).hasSize(43).isNotEqualTo(OpaqueTokens.generate());
        assertThat(OpaqueTokens.hash(raw)).isEqualTo(OpaqueTokens.hash(raw)).isNotEqualTo(raw);
    }

    @Test
    void clientIpIgnoresSpoofableForwardedFor() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "6.6.6.6");
        assertThat(ClientIp.of(req)).isEqualTo("10.0.0.1");
        req.addHeader("X-Real-IP", "1.2.3.4");
        assertThat(ClientIp.of(req)).isEqualTo("1.2.3.4");
        assertThat(ClientIp.current()).isNull();
    }

    @Test
    void googleTokensWithUnverifiedEmailAreRejected() throws Exception {
        var verifier = org.mockito.Mockito.mock(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class);
        var token = org.mockito.Mockito.mock(com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.class);
        var payload = new com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload().setEmail("victim@corp.com");
        org.mockito.Mockito.when(token.getPayload()).thenReturn(payload);
        org.mockito.Mockito.when(verifier.verify("t")).thenReturn(token);
        var google = new io.github.kushgarg132.kit.security.GoogleIdTokens(verifier);

        assertThatThrownBy(() -> google.verify("t"))
                .isInstanceOf(io.github.kushgarg132.kit.error.InvalidCredentialsException.class);
        payload.setEmailVerified(false);
        assertThatThrownBy(() -> google.verify("t"))
                .isInstanceOf(io.github.kushgarg132.kit.error.InvalidCredentialsException.class);
        payload.setEmailVerified(true);
        assertThat(google.verify("t").getEmail()).isEqualTo("victim@corp.com");
        org.mockito.Mockito.when(verifier.verify("bad")).thenReturn(null);
        assertThatThrownBy(() -> google.verify("bad"))
                .isInstanceOf(io.github.kushgarg132.kit.error.InvalidCredentialsException.class);
    }

    @RestController
    static class Boom {
        @GetMapping("/missing") String missing() { throw new ResourceNotFoundException("Habit", 42); }
        @GetMapping("/crash") String crash() { throw new IllegalStateException("secret internals"); }
    }

    @Test
    void handlerProducesTheErrorEnvelope() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new Boom()).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Habit not found: 42"))
                .andExpect(jsonPath("$.path").value("/missing"));
        mvc.perform(get("/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void autoConfigRegistersHandlerUnlessDisabled() {
        var runner = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(KitAutoConfiguration.class));
        runner.run(ctx -> assertThat(ctx).hasSingleBean(GlobalExceptionHandler.class));
        runner.withPropertyValues("kit.error-handler.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GlobalExceptionHandler.class));
    }
}
