package com.kinetiq.planchange.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** No drafting service is configured, so the fixed transformation is used. It never reaches an API response. */
public class PlanChangeDrafterUnavailableException extends DomainException {

    public PlanChangeDrafterUnavailableException() {
        super("no drafting service is configured");
    }

    @Override
    public String code() {
        return "plan-change.drafter-unavailable";
    }
}
