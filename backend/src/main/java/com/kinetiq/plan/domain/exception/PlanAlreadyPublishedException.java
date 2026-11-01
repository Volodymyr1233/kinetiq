package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A plan is published once; nothing moves it back to a draft. */
public class PlanAlreadyPublishedException extends DomainException {

    public PlanAlreadyPublishedException() {
        super("plan is already published");
    }

    @Override
    public String code() {
        return "plan.already-published";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
