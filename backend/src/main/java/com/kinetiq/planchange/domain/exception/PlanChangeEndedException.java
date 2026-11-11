package com.kinetiq.planchange.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The plan change is applied, declined or expired, so nobody can decide on it any more. */
public class PlanChangeEndedException extends DomainException {

    public PlanChangeEndedException() {
        super("plan change has ended");
    }

    @Override
    public String code() {
        return "plan-change.ended";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
