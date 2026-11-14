package com.kinetiq.notification.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.notification.domain.model.NotificationId;
import com.kinetiq.notification.domain.model.NotificationsView;

/** A person's own notifications. */
public interface ViewNotificationsUseCase {

    /** The person's latest notifications, newest first, with the unread count of all of them. */
    NotificationsView list(AccountId person);

    /**
     * Marks a notification of the person read; reading one that is read already changes nothing.
     *
     * @throws com.kinetiq.notification.domain.exception.NotificationNotFoundException when it is not the person's
     */
    void markRead(AccountId person, NotificationId id);
}
