package com.kinetiq.planchange.domain.model;

/** Where a plan change stands. An applied plan change is upcoming until the plan uses it, then in effect; see {@link PlanChange}. */
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
