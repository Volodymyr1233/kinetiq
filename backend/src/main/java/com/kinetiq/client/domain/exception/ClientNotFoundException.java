package com.kinetiq.client.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** One answer for an unknown client and another trainer's client. */
public class ClientNotFoundException extends DomainException {

    public ClientNotFoundException() {
        super("client is unknown or another trainer's");
    }

    @Override
    public String code() {
        return "client.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
