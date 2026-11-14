package com.kinetiq.notification.domain.model;

/** A notification with the current name of the client it is about; the name is null when there is none. */
public record NotificationView(Notification notification, String clientName) {}
