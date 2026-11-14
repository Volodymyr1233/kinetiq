package com.kinetiq.notification.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.notification.domain.exception.NotificationNotFoundException;
import com.kinetiq.notification.domain.input.RecordNoticeUseCase;
import com.kinetiq.notification.domain.input.ViewNotificationsUseCase;
import com.kinetiq.notification.domain.model.Notification;
import com.kinetiq.notification.domain.model.NotificationId;
import com.kinetiq.notification.domain.model.NotificationView;
import com.kinetiq.notification.domain.model.NotificationsView;
import com.kinetiq.notification.domain.output.ClientNames;
import com.kinetiq.notification.domain.output.NotificationEvents;
import com.kinetiq.notification.domain.output.NotificationRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationService implements ViewNotificationsUseCase, RecordNoticeUseCase {

    /** How many notifications the list shows. */
    public static final int LATEST = 30;

    private final NotificationRepository notifications;
    private final ClientNames names;
    private final NotificationEvents events;
    private final Clock clock;

    @Override
    public void record(Notice notice) {
        AccountId recipient = new AccountId(notice.recipient());
        notifications.save(new Notification(
                new NotificationId(UUID.randomUUID()),
                recipient,
                notice.kind(),
                notice.client() == null ? null : new AccountId(notice.client()),
                clock.instant(),
                null));
        events.stored(recipient);
    }

    @Override
    public NotificationsView list(AccountId person) {
        List<Notification> latest = notifications.latest(person, LATEST);
        Map<AccountId, String> clientNames = names.namesOf(latest.stream()
                .map(Notification::client)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        return new NotificationsView(
                latest.stream()
                        .map(notification -> new NotificationView(
                                notification,
                                notification.client() == null ? null : clientNames.get(notification.client())))
                        .toList(),
                notifications.countUnread(person));
    }

    @Override
    public void markRead(AccountId person, NotificationId id) {
        Notification notification = notifications.findOf(person, id).orElseThrow(NotificationNotFoundException::new);
        if (!notification.isRead()) {
            notifications.save(notification.readAt(clock.instant()));
        }
    }
}
