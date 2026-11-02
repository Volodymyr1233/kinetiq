package com.kinetiq.messaging.domain.model;

/** Where a client is in their published plan; {@code current} is above {@code total} once the plan has finished. */
public record PlanWeek(int current, int total) {}
