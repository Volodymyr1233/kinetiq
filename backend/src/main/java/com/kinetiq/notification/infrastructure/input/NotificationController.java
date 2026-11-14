package com.kinetiq.notification.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.notification.domain.input.ViewNotificationsUseCase;
import com.kinetiq.notification.domain.model.NotificationId;
import com.kinetiq.notification.domain.model.NotificationsView;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// any signed-in person; the notifications are always the signed-in person's own, so no id of a person is taken
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
class NotificationController {

    private final ViewNotificationsUseCase notifications;

    /** The signed-in person's latest 30 notifications, newest first, with their unread count. */
    @GetMapping
    NotificationListResponse list(Authentication authentication) {
        NotificationsView view = notifications.list(person(authentication));
        return new NotificationListResponse(
                view.items().stream()
                        .map(item -> new NotificationResponse(
                                item.notification().id().value(),
                                item.notification().kind(),
                                item.notification().createdAt(),
                                item.notification().isRead(),
                                // a client who is gone leaves no id behind
                                item.clientName() == null || item.notification().client() == null
                                        ? null
                                        : item.notification().client().value(),
                                item.clientName()))
                        .toList(),
                view.unread());
    }

    /** Marks one of the signed-in person's notifications read; {@code 404} for anyone else's. */
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@PathVariable UUID id, Authentication authentication) {
        notifications.markRead(person(authentication), new NotificationId(id));
    }

    // sign-in stores the account id as the principal's name
    private static AccountId person(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }
}
