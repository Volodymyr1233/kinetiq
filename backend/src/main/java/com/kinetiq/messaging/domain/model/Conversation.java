package com.kinetiq.messaging.domain.model;

import java.util.Objects;
import lombok.Getter;

/** All messages between one client and their trainer. It is stored from the first message on. */
@Getter
public class Conversation {

    private final ConversationId id;
    private final ConversationPair pair;

    public Conversation(ConversationId id, ConversationPair pair) {
        this.id = Objects.requireNonNull(id);
        this.pair = Objects.requireNonNull(pair);
    }
}
