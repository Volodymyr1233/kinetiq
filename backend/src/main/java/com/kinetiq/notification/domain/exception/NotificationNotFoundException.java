package com.kinetiq.notification.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The notification is not this person's, or does not exist. */
public class NotificationNotFoundException extends DomainException {

    public NotificationNotFoundException() {
        super("the notification is not this person's");
    }

    @Override
    public String code() {
        return "notification.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
