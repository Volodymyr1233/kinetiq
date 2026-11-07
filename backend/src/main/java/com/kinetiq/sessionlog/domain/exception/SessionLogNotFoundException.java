package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The session is unknown, or another client's, or a set id belongs to another session; the three look the same. */
public class SessionLogNotFoundException extends DomainException {

    public SessionLogNotFoundException() {
        super("session log not found");
    }

    @Override
    public String code() {
        return "session-log.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
