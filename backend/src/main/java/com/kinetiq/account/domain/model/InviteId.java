package com.kinetiq.account.domain.model;

import java.util.Objects;
import java.util.UUID;

public record InviteId(UUID value) {

    public InviteId {
        Objects.requireNonNull(value);
    }

    public static InviteId random() {
        return new InviteId(UUID.randomUUID());
    }
}
