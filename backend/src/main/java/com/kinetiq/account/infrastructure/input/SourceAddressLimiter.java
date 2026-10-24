package com.kinetiq.account.infrastructure.input;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Allows at most 10 requests of one kind from one network address in any hour. */
// ponytail: counts are per instance and lost on restart; a shared store is the upgrade once there are several
@Component
class SourceAddressLimiter {

    enum Kind {
        SIGN_UP,
        CONFIRMATION_LINK
    }

    private static final int LIMIT = 10;
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int PRUNE_ABOVE = 10_000;

    private record Key(Kind kind, String address) {}

    private final Map<Key, Deque<Instant>> requests = new ConcurrentHashMap<>();
    private final Clock clock;

    SourceAddressLimiter() {
        this(Clock.systemUTC());
    }

    SourceAddressLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records the request and tells whether it is within the limit. */
    boolean tryAcquire(Kind kind, String address) {
        Instant now = clock.instant();
        Instant oldest = now.minus(WINDOW);
        if (requests.size() > PRUNE_ABOVE) {
            requests.values().removeIf(times -> isStale(times, oldest));
        }
        Deque<Instant> times = requests.computeIfAbsent(new Key(kind, address), key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && !times.peekFirst().isAfter(oldest)) {
                times.pollFirst();
            }
            if (times.size() >= LIMIT) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    private static boolean isStale(Deque<Instant> times, Instant oldest) {
        synchronized (times) {
            return times.isEmpty() || !times.peekLast().isAfter(oldest);
        }
    }
}
