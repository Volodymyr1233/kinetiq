package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The trainer's own client has no plan; another trainer's client never gets this answer. */
public class PlanNotFoundException extends DomainException {

    public PlanNotFoundException() {
        super("client has no plan");
    }

    @Override
    public String code() {
        return "plan.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
