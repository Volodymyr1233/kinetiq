package com.kinetiq.account.domain.model;

import java.time.Instant;
import java.util.Objects;

/** The one confirmation link of an account that still works. */
public record EmailConfirmation(String tokenHash, Instant expiresAt) {

    public EmailConfirmation {
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(expiresAt);
    }
}
