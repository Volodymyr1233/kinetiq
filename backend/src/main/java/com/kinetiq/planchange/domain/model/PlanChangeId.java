package com.kinetiq.planchange.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PlanChangeId(UUID value) {

    public PlanChangeId {
        Objects.requireNonNull(value);
    }

    public static PlanChangeId random() {
        return new PlanChangeId(UUID.randomUUID());
    }
}
