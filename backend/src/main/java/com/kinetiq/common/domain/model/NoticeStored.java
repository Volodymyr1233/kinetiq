package com.kinetiq.common.domain.model;

import java.util.Objects;
import java.util.UUID;

/** A notification was stored for this person; their open streams are told once the transaction commits. */
public record NoticeStored(UUID recipient) {

    public NoticeStored {
        Objects.requireNonNull(recipient);
    }
}
