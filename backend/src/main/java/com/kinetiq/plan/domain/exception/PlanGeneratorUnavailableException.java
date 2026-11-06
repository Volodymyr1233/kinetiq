package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** No drafting service is configured. It ends in a failed draft and never reaches an API response. */
public class PlanGeneratorUnavailableException extends DomainException {

    public PlanGeneratorUnavailableException() {
        super("no drafting service is configured");
    }

    @Override
    public String code() {
        return "plan.generator-unavailable";
    }
}
