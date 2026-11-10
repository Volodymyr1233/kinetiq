package com.kinetiq.planchange.domain.model;

import java.util.Objects;

/** What a plan change says in one language: what changes, and why. */
public record PlanChangeText(String title, String reason) {

    public PlanChangeText {
        Objects.requireNonNull(title);
        Objects.requireNonNull(reason);
    }
}
