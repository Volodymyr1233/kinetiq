package com.kinetiq.messaging.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** One answer for an unknown client, another trainer's client and a third person. */
public class ConversationNotFoundException extends DomainException {

    public ConversationNotFoundException() {
        super("the conversation is unknown or another person's");
    }

    @Override
    public String code() {
        return "conversation.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
