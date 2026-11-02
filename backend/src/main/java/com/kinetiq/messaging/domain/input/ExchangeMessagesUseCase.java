package com.kinetiq.messaging.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import java.util.List;

/**
 * What a client and their trainer do with their conversation. A conversation is named by its client; {@code person} is
 * the signed-in person, who must be that client or their current trainer.
 */
public interface ExchangeMessagesUseCase {

    /** Lists the person's conversations: a trainer's row per linked client, or a client's one row. */
    List<ConversationOverview> listConversations(AccountId person);

    /** Returns how many unread messages the person has over all their conversations. */
    int unreadCount(AccountId person);

    /**
     * Returns one page of a conversation, empty while nobody has written.
     *
     * @param before the message to read back from, or {@code null} for the newest page
     */
    MessagePage messages(AccountId person, AccountId client, MessageId before);

    /** Sends a message, or returns the stored one when this id was sent before. */
    Message send(SendMessageCommand command);

    /** Marks the conversation read for the person. Nothing happens while nobody has written. */
    void markRead(AccountId person, AccountId client);
}
