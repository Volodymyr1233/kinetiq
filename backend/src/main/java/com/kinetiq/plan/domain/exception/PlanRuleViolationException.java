package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A plan that breaks one of the plan rules; the code says which. */
public class PlanRuleViolationException extends DomainException {

    private final String code;

    public PlanRuleViolationException(String code) {
        super("plan rule broken: " + code);
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
