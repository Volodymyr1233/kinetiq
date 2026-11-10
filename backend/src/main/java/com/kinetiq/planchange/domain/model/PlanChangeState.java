package com.kinetiq.planchange.domain.model;

/** Where a plan change stands. Only {@code PENDING} is written so far; the others belong to deciding and applying. */
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
