package com.kinetiq.messaging.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The message is not part of this conversation, or does not exist. */
public class MessageNotFoundException extends DomainException {

    public MessageNotFoundException() {
        super("the message is not in this conversation");
    }

    @Override
    public String code() {
        return "message.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
