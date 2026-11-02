package com.kinetiq.messaging.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.MessageId;

/**
 * One attempt to send a message: {@code person} writes to the conversation of {@code client}. {@code id} is the same
 * for every attempt of one message.
 */
public record SendMessageCommand(AccountId person, AccountId client, MessageId id, String text) {}
