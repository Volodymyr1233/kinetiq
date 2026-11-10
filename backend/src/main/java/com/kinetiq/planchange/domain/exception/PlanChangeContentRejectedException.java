package com.kinetiq.planchange.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;
import java.util.Set;

/**
 * An answer that cannot become plan change content, with the limit code of each reason. It leads to a second attempt or
 * the fixed transformation and never reaches an API response.
 */
public class PlanChangeContentRejectedException extends DomainException {

    private final Set<String> codes;

    public PlanChangeContentRejectedException(Set<String> codes) {
        super("the answer breaks " + codes);
        this.codes = Set.copyOf(codes);
    }

    public Set<String> codes() {
        return codes;
    }

    @Override
    public String code() {
        return "plan-change.content-rejected";
    }
}
