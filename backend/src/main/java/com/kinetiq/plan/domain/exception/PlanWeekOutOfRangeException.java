package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A client asked for a plan week that does not exist: outside the plan, or any week of a finished plan. */
public class PlanWeekOutOfRangeException extends DomainException {

    public PlanWeekOutOfRangeException() {
        super("plan week out of range");
    }

    @Override
    public String code() {
        return "plan.week-out-of-range";
    }

    @Override
    public Kind kind() {
        return Kind.INVALID;
    }
}
