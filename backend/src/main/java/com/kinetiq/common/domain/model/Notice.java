package com.kinetiq.common.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * One thing a person is to be told. {@code client} is the client it is about, when it is about one and may name them
 * to the recipient; it is null for {@link NoticeKind#CLIENT_DELETED}, which keeps nothing of the client. Plain ids
 * keep the shared kernel free of the account feature.
 */
public record Notice(UUID recipient, UUID client, NoticeKind kind) {

    public Notice {
        Objects.requireNonNull(recipient);
        Objects.requireNonNull(kind);
    }
}
