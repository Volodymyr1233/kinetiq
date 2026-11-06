package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A first save arrived while the client's first plan is being drafted, and would collide with the draft. */
public class PlanDraftingInProgressException extends DomainException {

    public PlanDraftingInProgressException() {
        super("the client's first plan is being drafted");
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
