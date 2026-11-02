package com.kinetiq.messaging.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/** A piece of text sent by one of the two people of a conversation. It never changes. */
@Getter
public class Message {

    private final MessageId id;
    private final ConversationId conversation;
    private final AccountId sender;
    private final MessageText text;
    private final Instant receivedAt;

    public Message(MessageId id, ConversationId conversation, AccountId sender, MessageText text, Instant receivedAt) {
        this.id = Objects.requireNonNull(id);
        this.conversation = Objects.requireNonNull(conversation);
        this.sender = Objects.requireNonNull(sender);
        this.text = Objects.requireNonNull(text);
        this.receivedAt = Objects.requireNonNull(receivedAt);
    }

    /** Tells whether {@code other} is this message sent again: the same conversation, sender and text. */
    public boolean isSameSendAs(Message other) {
        return conversation.equals(other.conversation) && sender.equals(other.sender) && text.equals(other.text);
    }
}
