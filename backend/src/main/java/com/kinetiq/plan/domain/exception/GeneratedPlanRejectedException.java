package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;
import java.util.Collection;
import java.util.List;

/** An answer of the drafting service broke the checks. It leads to another attempt and never reaches an API response. */
public class GeneratedPlanRejectedException extends DomainException {

    private final List<String> codes;

    public GeneratedPlanRejectedException(Collection<String> codes) {
        super("generated plan rejected: " + codes);
        this.codes = List.copyOf(codes);
    }

    /** Every check the answer broke. */
    public List<String> codes() {
        return codes;
    }

    @Override
    public String code() {
        return "plan.generated-rejected";
    }
}
