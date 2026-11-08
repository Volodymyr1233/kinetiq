package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The session is not finished, so it has no summary. */
public class NotFinishedException extends DomainException {

    public NotFinishedException() {
        super("session log not finished");
    }

    @Override
    public String code() {
        return "session-log.not-finished";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
