package com.kinetiq.notification.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** The latest notifications, newest first, and the number of unread ones among all of the person's. */
record NotificationListResponse(@NotNull List<NotificationResponse> items, @NotNull long unread) {}
