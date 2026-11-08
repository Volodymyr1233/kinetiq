package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The session started 24 hours ago or more, so its sets can no longer be logged or changed. */
public class SessionLogLockedException extends DomainException {

    public SessionLogLockedException() {
        super("session log locked");
    }

    @Override
    public String code() {
        return "session-log.locked";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
