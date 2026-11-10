package com.kinetiq.planchange.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/**
 * An attempt at plan change content gave no usable answer. The reason is written by this application and holds no
 * text from the service; it never reaches an API response.
 */
public class PlanChangeDraftingFailedException extends DomainException {

    public PlanChangeDraftingFailedException(String reason) {
        super(reason);
    }

    @Override
    public String code() {
        return "plan-change.drafting-failed";
    }
}
