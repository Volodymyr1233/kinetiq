package com.kinetiq.planchange.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** Someone tried to undo an acceptance that they never gave, or that cannot be taken back. */
public class PlanChangeNothingToUndoException extends DomainException {

    public PlanChangeNothingToUndoException() {
        super("nothing to undo");
    }

    @Override
    public String code() {
        return "plan-change.nothing-to-undo";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
