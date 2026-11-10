package com.kinetiq.messaging.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.sessionlog.domain.model.SetLogId;

/**
 * One attempt to send a message: {@code person} writes to the conversation of {@code client}. {@code id} is the same
 * for every attempt of one message. {@code about} is the sender's own logged set the message is about, or {@code null}.
 */
public record SendMessageCommand(AccountId person, AccountId client, MessageId id, String text, SetLogId about) {

    public SendMessageCommand(AccountId person, AccountId client, MessageId id, String text) {
        this(person, client, id, text, null);
    }
}
