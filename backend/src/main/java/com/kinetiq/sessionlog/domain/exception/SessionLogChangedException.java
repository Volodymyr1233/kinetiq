package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** Another request changed the session at the same moment; repeating the request answers on the new state. */
public class SessionLogChangedException extends DomainException {

    public SessionLogChangedException() {
        super("session log changed since it was read");
    }

    @Override
    public String code() {
        return "session-log.changed";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
