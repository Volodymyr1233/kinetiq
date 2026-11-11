package com.kinetiq.planchange.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The effective week of the plan change has begun, so it can no longer be decided. */
public class PlanChangeWeekBegunException extends DomainException {

    public PlanChangeWeekBegunException() {
        super("effective week has begun");
    }

    @Override
    public String code() {
        return "plan-change.week-begun";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
