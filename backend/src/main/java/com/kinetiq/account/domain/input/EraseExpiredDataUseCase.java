package com.kinetiq.account.domain.input;

/** Erases the personal data nobody uses any more, as the data inventory (ADR 0005) says. */
public interface EraseExpiredDataUseCase {

    /**
     * Erases trainer accounts whose email was never confirmed 7 days after their last confirmation link was sent, and
     * invites 1 hour after they were used, revoked or expired. Client accounts are never erased here.
     */
    void eraseExpired();
}
