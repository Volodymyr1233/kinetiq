package com.kinetiq.sessionlog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SessionLogId(UUID value) {

    public SessionLogId {
        Objects.requireNonNull(value);
    }

    public static SessionLogId random() {
        return new SessionLogId(UUID.randomUUID());
    }
}
