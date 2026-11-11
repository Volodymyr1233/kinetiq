package com.kinetiq.planchange.domain.model;

/** Where a plan change stands. {@code EXPIRED} is written by the story that applies and expires plan changes. */
public enum PlanChangeState {
    PENDING,
    ACCEPTED_BY_ONE,
    APPLIED,
    DECLINED,
    EXPIRED;

    /** Whether the plan change has not ended yet. */
    public boolean isOpen() {
        return this == PENDING || this == ACCEPTED_BY_ONE;
    }
}
