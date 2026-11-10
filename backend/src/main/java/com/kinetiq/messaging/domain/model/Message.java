package com.kinetiq.messaging.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;

/**
 * A piece of text sent by one of the two people of a conversation, which may be about one logged set of its sender. It
 * never changes.
 */
@Getter
public class Message {

    private final MessageId id;
    private final ConversationId conversation;
    private final AccountId sender;
    private final MessageText text;
    private final Instant receivedAt;
    private final AboutSet about;

    public Message(MessageId id, ConversationId conversation, AccountId sender, MessageText text, Instant receivedAt) {
        this(id, conversation, sender, text, receivedAt, null);
    }

    /** @param about the logged set the message is about, or {@code null} */
    public Message(
            MessageId id,
            ConversationId conversation,
            AccountId sender,
            MessageText text,
            Instant receivedAt,
            AboutSet about) {
        this.id = Objects.requireNonNull(id);
        this.conversation = Objects.requireNonNull(conversation);
        this.sender = Objects.requireNonNull(sender);
        this.text = Objects.requireNonNull(text);
        this.receivedAt = Objects.requireNonNull(receivedAt);
        this.about = about;
    }

    public Optional<AboutSet> getAbout() {
        return Optional.ofNullable(about);
    }

    /** Tells whether {@code other} is this message sent again: the same conversation, sender, text and set. */
    public boolean isSameSendAs(Message other) {
        return conversation.equals(other.conversation)
                && sender.equals(other.sender)
                && text.equals(other.text)
                && getAbout().map(AboutSet::setLog).equals(other.getAbout().map(AboutSet::setLog));
    }
}
