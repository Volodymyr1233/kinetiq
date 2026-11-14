package com.kinetiq.notification.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.notification.domain.model.Notification;
import com.kinetiq.notification.domain.model.NotificationId;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    /** The most recent {@code limit} notifications of the person, newest first. */
    List<Notification> latest(AccountId person, int limit);

    long countUnread(AccountId person);

    /** The notification, only when it is the person's. */
    Optional<Notification> findOf(AccountId person, NotificationId id);
}
