package com.kinetiq.account.domain.model;

import java.time.Instant;
import java.util.Objects;

/** The one password reset link of an account that still works. */
public record PasswordReset(String tokenHash, Instant expiresAt) {

    public PasswordReset {
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(expiresAt);
    }
}
