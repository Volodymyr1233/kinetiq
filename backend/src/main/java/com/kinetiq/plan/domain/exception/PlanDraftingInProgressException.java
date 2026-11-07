package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A change or a new drafting arrived while the client's plan is being drafted, and would collide with its result. */
public class PlanDraftingInProgressException extends DomainException {

    public PlanDraftingInProgressException() {
        super("the client's plan is being drafted");
    }

    @Override
    public String code() {
        return "plan.drafting";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
