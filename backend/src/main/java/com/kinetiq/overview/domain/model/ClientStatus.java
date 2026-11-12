package com.kinetiq.overview.domain.model;

/** The trainer's at-a-glance verdict on a client, declared most urgent first, the order the trainer's lists sort in. */
public enum ClientStatus {
    AT_RISK,
    WATCH,
    PLAN_TO_REVIEW,
    NO_PLAN,
    ON_TRACK
}
