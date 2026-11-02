package com.kinetiq.messaging.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import java.util.Objects;

/** The two people of a conversation: a client and the trainer they train under. */
public record ConversationPair(AccountId client, AccountId trainer) {

    public ConversationPair {
        Objects.requireNonNull(client);
        Objects.requireNonNull(trainer);
        if (client.equals(trainer)) {
            throw new IllegalArgumentException("a conversation needs two people");
        }
    }

    public boolean includes(AccountId person) {
        return client.equals(person) || trainer.equals(person);
    }
}
