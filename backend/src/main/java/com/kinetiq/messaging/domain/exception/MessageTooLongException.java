package com.kinetiq.messaging.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A message holds more than {@link com.kinetiq.messaging.domain.model.MessageText#MAX_LENGTH} UTF-16 units. */
public class MessageTooLongException extends DomainException {

    public MessageTooLongException() {
        super("the message is too long");
    }

    @Override
    public String code() {
        return "message.too-long";
    }
}
