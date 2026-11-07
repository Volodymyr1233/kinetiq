package com.kinetiq.sessionlog.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Made by the client when it logs a set, so a send that is repeated stores the set once. */
public record SetLogId(UUID value) {

    public SetLogId {
        Objects.requireNonNull(value);
    }
}
