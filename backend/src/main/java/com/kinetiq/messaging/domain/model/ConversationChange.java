package com.kinetiq.messaging.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import java.util.Objects;

/**
 * One notice for one person that one of their conversations changed. The conversation is named by its client, as
 * everywhere in messaging.
 */
public record ConversationChange(AccountId person, AccountId client, Kind kind) {

    /** What happened in the conversation. */
    public enum Kind {
        /** A message was stored in it. */
        MESSAGE,
        /** The person told marked it read. */
        READ
    }

    public ConversationChange {
        Objects.requireNonNull(person);
        Objects.requireNonNull(client);
        Objects.requireNonNull(kind);
    }
}
