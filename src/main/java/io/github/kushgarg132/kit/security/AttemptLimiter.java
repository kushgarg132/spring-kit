package io.github.kushgarg132.kit.security;

import io.github.kushgarg132.kit.error.TooManyAttemptsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding-window limiter on failed attempts per key (email, IP, ...), on top of Nginx's
 * edge rate limits. ponytail: single-instance only; needs a shared store (Redis) if a backend ever
 * runs more than one replica.
 */
public class AttemptLimiter {

    /** Bounds memory when an attacker cycles through made-up keys. */
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final int maxFailures;
    private final Duration window;
    private final String message;
    private final Clock clock;
    private final Map<String, ConcurrentLinkedDeque<Instant>> failures = new ConcurrentHashMap<>();

    public AttemptLimiter(int maxFailures, Duration window, String message) {
        this(maxFailures, window, message, Clock.systemUTC());
    }

    public AttemptLimiter(int maxFailures, Duration window, String message, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.message = message;
        this.clock = clock;
    }

    /** @throws TooManyAttemptsException once {@code maxFailures} failures fall inside the window. */
    public void checkAllowed(String key) {
        ConcurrentLinkedDeque<Instant> times = failures.get(normalize(key));
        if (times == null) {
            return;
        }
        prune(times);
        if (times.size() >= maxFailures) {
            throw new TooManyAttemptsException(message);
        }
    }

    public void recordFailure(String key) {
        if (failures.size() >= MAX_TRACKED_KEYS) {
            failures.clear();
        }
        ConcurrentLinkedDeque<Instant> times = failures.computeIfAbsent(normalize(key), k -> new ConcurrentLinkedDeque<>());
        times.addLast(clock.instant());
        prune(times);
    }

    public void recordSuccess(String key) {
        failures.remove(normalize(key));
    }

    private void prune(ConcurrentLinkedDeque<Instant> times) {
        Instant cutoff = clock.instant().minus(window);
        for (Instant oldest = times.peekFirst(); oldest != null && !oldest.isAfter(cutoff); oldest = times.peekFirst()) {
            times.pollFirst();
        }
    }

    private static String normalize(String key) {
        return key == null ? "" : key.toLowerCase();
    }
}
