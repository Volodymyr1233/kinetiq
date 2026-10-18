package com.kinetiq.account.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value);
    }

    public static AccountId random() {
        return new AccountId(UUID.randomUUID());
    }
}
