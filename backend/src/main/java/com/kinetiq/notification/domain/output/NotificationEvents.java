package com.kinetiq.notification.domain.output;

import com.kinetiq.account.domain.model.AccountId;

/** Tells a person's open streams that a notification was stored for them. */
public interface NotificationEvents {

    /**
     * Tells every open stream of {@code recipient}, once the current transaction has committed. It never fails the
     * caller: a stream that cannot be told is closed.
     */
    void stored(AccountId recipient);
}
