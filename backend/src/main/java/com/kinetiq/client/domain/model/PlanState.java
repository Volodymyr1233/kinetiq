package com.kinetiq.client.domain.model;

/** Where a client stands with the plan, as the trainer's client list shows it. */
public enum PlanState {
    NO_QUIZ,
    PLAN_DRAFTING,
    PLAN_TO_REVIEW,
    DRAFT,
    DRAFT_FAILED,
    PUBLISHED
}
