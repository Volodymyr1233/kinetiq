package com.kinetiq.notification.domain.model;

import java.util.List;

/** The latest notifications of a person, newest first, and how many of all theirs are unread. */
public record NotificationsView(List<NotificationView> items, long unread) {

    public NotificationsView {
        items = List.copyOf(items);
    }
}
