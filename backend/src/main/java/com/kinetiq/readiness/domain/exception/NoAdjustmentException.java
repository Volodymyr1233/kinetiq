package com.kinetiq.readiness.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** Today's targets are as planned, so there is nothing to switch between. */
public class NoAdjustmentException extends DomainException {

    public NoAdjustmentException() {
        super("no adjustment");
    }

    @Override
    public String code() {
        return "check-in.no-adjustment";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
