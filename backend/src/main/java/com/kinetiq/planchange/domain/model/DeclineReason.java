package com.kinetiq.planchange.domain.model;

/** Why a person kept the current plan instead of accepting a plan change. */
public enum DeclineReason {
    TOO_EASY,
    TOO_HARD,
    SCHEDULE,
    INJURY,
    OTHER
}
