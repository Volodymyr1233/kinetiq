package com.kinetiq.notification.domain.model;

import java.util.Objects;
import java.util.UUID;

public record NotificationId(UUID value) {

    public NotificationId {
        Objects.requireNonNull(value);
    }
}
