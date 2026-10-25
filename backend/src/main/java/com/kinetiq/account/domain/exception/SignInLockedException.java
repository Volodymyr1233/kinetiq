package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;
import java.time.Duration;
import java.util.Map;

/** Given for every locked email, with or without an account, so it reveals nothing about accounts. */
public class SignInLockedException extends DomainException {

    private final Duration timeLeft;

    public SignInLockedException(Duration timeLeft) {
        super("too many failed sign-ins for this email");
        this.timeLeft = timeLeft;
    }

    @Override
    public String code() {
        return "account.sign-in-locked";
    }

    @Override
    public Kind kind() {
        return Kind.TOO_MANY_ATTEMPTS;
    }

    // rounded up, so the page never promises a shorter wait than the real one
    @Override
    public Map<String, Object> properties() {
        long seconds = timeLeft.toSeconds() + (timeLeft.toNanosPart() > 0 ? 1 : 0);
        return Map.of("retryAfterSeconds", Math.max(1, seconds));
    }
}
