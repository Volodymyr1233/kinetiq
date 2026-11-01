package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;
import java.util.Map;

/** A published plan shortened to below the plan week it is already in. */
public class PlanWeeksBeforeCurrentException extends DomainException {

    private final int currentWeek;

    public PlanWeeksBeforeCurrentException(int currentWeek) {
        super("plan shortened below its current week");
        this.currentWeek = currentWeek;
    }

    @Override
    public String code() {
        return "plan.weeks-before-current";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("currentWeek", currentWeek);
    }
}
