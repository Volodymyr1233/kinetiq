package com.kinetiq.messaging.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The id belongs to a message with another conversation, sender or text, so this is not the same send repeated. */
public class MessageIdInUseException extends DomainException {

    public MessageIdInUseException() {
        super("the message id is in use");
    }

    @Override
    public String code() {
        return "message.id-in-use";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
