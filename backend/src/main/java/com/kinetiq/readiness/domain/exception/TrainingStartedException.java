package com.kinetiq.readiness.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A set of today's session is logged, so today's check-in can no longer be given or changed. */
public class TrainingStartedException extends DomainException {

    public TrainingStartedException() {
        super("training started");
    }

    @Override
    public String code() {
        return "check-in.training-started";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
