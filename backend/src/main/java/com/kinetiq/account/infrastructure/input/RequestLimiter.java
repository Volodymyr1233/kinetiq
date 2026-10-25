package com.kinetiq.account.infrastructure.input;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Allows at most a fixed number of requests of one kind per key, such as a network address, in any hour. */
// ponytail: counts are per instance and lost on restart; a shared store is the upgrade once there are several
@Component
class RequestLimiter {

    enum Kind {
        SIGN_UP(10),
        CONFIRMATION_LINK(10),
        INVITE(20);

        private final int limit;

        Kind(int limit) {
            this.limit = limit;
        }
    }

    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int PRUNE_ABOVE = 10_000;

    private record Key(Kind kind, String value) {}

    private final Map<Key, Deque<Instant>> requests = new ConcurrentHashMap<>();
    private final Clock clock;

    RequestLimiter() {
        this(Clock.systemUTC());
    }

    RequestLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records the request and tells whether it is within the limit. */
    boolean tryAcquire(Kind kind, String key) {
        Instant now = clock.instant();
        Instant oldest = now.minus(WINDOW);
        if (requests.size() > PRUNE_ABOVE) {
            requests.values().removeIf(times -> isStale(times, oldest));
        }
        Deque<Instant> times = requests.computeIfAbsent(new Key(kind, key), absent -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && !times.peekFirst().isAfter(oldest)) {
                times.pollFirst();
            }
            if (times.size() >= kind.limit) {
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
