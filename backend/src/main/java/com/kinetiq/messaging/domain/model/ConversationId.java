package com.kinetiq.messaging.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ConversationId(UUID value) {

    public ConversationId {
        Objects.requireNonNull(value);
    }

    public static ConversationId random() {
        return new ConversationId(UUID.randomUUID());
    }
}
