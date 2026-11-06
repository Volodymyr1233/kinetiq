package com.kinetiq.plan.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the drafting service answered, not yet trusted: nothing here is validated. Numbers are boxed so that a missing
 * value stays visible and is not read as zero.
 */
public record GeneratedPlan(List<GeneratedSession> sessions) {

    /** A session as answered; {@code day} should name a day of the week in capitals, such as {@code MONDAY}. */
    public record GeneratedSession(String name, String day, List<GeneratedEntry> entries) {}

    /** An exercise's targets as answered; {@code exerciseKey} means something only within the prompt that asked. */
    public record GeneratedEntry(
            String exerciseKey, Integer sets, Integer amount, BigDecimal loadKg, Integer rpe, Integer restSeconds) {}
}
