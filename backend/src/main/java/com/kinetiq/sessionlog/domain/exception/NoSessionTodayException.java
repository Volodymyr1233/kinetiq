package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The client's plan has no session today: a rest day, or the plan has finished. */
public class NoSessionTodayException extends DomainException {

    public NoSessionTodayException() {
        super("no session today");
    }

    @Override
    public String code() {
        return "session-log.no-session-today";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
