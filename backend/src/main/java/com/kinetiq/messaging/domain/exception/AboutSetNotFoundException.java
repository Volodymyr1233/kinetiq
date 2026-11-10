package com.kinetiq.messaging.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The set a message was to be about is not a logged set of its sender, or does not exist. */
public class AboutSetNotFoundException extends DomainException {

    public AboutSetNotFoundException() {
        super("the set log is not the sender's");
    }

    @Override
    public String code() {
        return "message.about-not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
