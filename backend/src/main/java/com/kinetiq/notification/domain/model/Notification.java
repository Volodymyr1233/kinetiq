package com.kinetiq.notification.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.NoticeKind;
import java.time.Instant;
import java.util.Objects;

/**
 * A notice to one person. {@code client} is the client it is about and {@code readAt} the moment the person read it;
 * both are null when there is none.
 */
public record Notification(
        NotificationId id, AccountId recipient, NoticeKind kind, AccountId client, Instant createdAt, Instant readAt) {

    public Notification {
        Objects.requireNonNull(id);
        Objects.requireNonNull(recipient);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(createdAt);
    }

    public boolean isRead() {
        return readAt != null;
    }

    /** This notification, read at {@code at}; one that is read already stays as it is. */
    public Notification readAt(Instant at) {
        return isRead() ? this : new Notification(id, recipient, kind, client, createdAt, at);
    }
}
