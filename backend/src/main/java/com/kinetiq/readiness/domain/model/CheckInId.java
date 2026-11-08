package com.kinetiq.readiness.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CheckInId(UUID value) {

    public CheckInId {
        Objects.requireNonNull(value);
    }

    public static CheckInId random() {
        return new CheckInId(UUID.randomUUID());
    }
}
