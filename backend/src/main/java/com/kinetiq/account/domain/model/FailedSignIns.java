package com.kinetiq.account.domain.model;

import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;

/**
 * Failed sign-ins for one email, with or without an account. Five failures within 15 minutes lock the email for 15
 * minutes from the fifth.
 */
public class FailedSignIns {

    public static final int MAX_FAILURES = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);
    public static final Duration LOCK = Duration.ofMinutes(15);

    @Getter
    private final Email email;

    private final List<Instant> failures = new ArrayList<>();
    private Instant lockedUntil;

    public FailedSignIns(Email email) {
        this.email = Objects.requireNonNull(email);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** How long the lock still lasts; empty when the email is not locked. */
    public Optional<Duration> timeLeft(Instant now) {
        return isLocked(now) ? Optional.of(Duration.between(now, lockedUntil)) : Optional.empty();
    }

    /** Counts a failure; attempts while locked neither count nor extend the lock. */
    public void recordFailure(Instant now) {
        if (isLocked(now)) {
            return;
        }
        failures.removeIf(failure -> !failure.isAfter(now.minus(WINDOW)));
        failures.add(now);
        if (failures.size() >= MAX_FAILURES) {
            lockedUntil = now.plus(LOCK);
            failures.clear();
        }
    }

    /** Nothing left to remember: not locked and no failure within the window. */
    public boolean isExpired(Instant now) {
        return !isLocked(now) && failures.stream().noneMatch(failure -> failure.isAfter(now.minus(WINDOW)));
    }
}
