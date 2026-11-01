package com.kinetiq.plan.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PlanId(UUID value) {

    public PlanId {
        Objects.requireNonNull(value);
    }

    public static PlanId random() {
        return new PlanId(UUID.randomUUID());
    }
}
