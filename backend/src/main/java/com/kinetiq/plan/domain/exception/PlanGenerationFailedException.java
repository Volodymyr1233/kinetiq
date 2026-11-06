package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/**
 * An attempt gave no usable answer. The reason is written by this application and holds no text from the service. It
 * ends in a failed draft and never reaches an API response.
 */
public class PlanGenerationFailedException extends DomainException {

    public PlanGenerationFailedException(String reason) {
        super(reason);
    }

    @Override
    public String code() {
        return "plan.generation-failed";
    }
}
