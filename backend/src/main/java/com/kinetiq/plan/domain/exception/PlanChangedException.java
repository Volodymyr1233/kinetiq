package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The plan changed since this editor loaded it, or another first save got there first. */
public class PlanChangedException extends DomainException {

    public PlanChangedException() {
        super("plan changed since it was read");
    }

    @Override
    public String code() {
        return "plan.changed";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
