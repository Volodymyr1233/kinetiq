package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A session with no logged set cannot be finished. */
public class NothingLoggedException extends DomainException {

    public NothingLoggedException() {
        super("nothing logged in the session");
    }

    @Override
    public String code() {
        return "session-log.nothing-logged";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
