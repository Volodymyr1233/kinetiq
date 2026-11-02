package com.kinetiq.messaging.domain.model;

import java.util.List;

/** One page of a conversation, oldest to newest, and whether earlier messages exist. */
public record MessagePage(List<Message> messages, boolean hasMore) {

    public static final int SIZE = 50;

    public MessagePage {
        messages = List.copyOf(messages);
    }

    public static MessagePage empty() {
        return new MessagePage(List.of(), false);
    }
}
