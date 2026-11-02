package com.kinetiq.messaging.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** Nothing is left of a message after removing the spaces and line breaks around it. */
public class MessageEmptyException extends DomainException {

    public MessageEmptyException() {
        super("the message is empty");
    }

    @Override
    public String code() {
        return "message.empty";
    }
}
