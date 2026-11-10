package com.kinetiq.planchange.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** One answer for an unknown plan change, another trainer's client's, and a client with no open one. */
public class PlanChangeNotFoundException extends DomainException {

    public PlanChangeNotFoundException() {
        super("plan change is unknown, another trainer's or not open");
    }

    @Override
    public String code() {
        return "plan-change.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
