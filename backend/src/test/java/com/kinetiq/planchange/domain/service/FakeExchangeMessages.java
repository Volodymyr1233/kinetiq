package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.messaging.domain.input.SendMessageCommand;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Keeps what was sent; sending fails while {@code failure} is set. */
class FakeExchangeMessages implements ExchangeMessagesUseCase {

    final List<SendMessageCommand> sent = new ArrayList<>();
    RuntimeException failure;

    @Override
    public Message send(SendMessageCommand command) {
        if (failure != null) {
            throw failure;
        }
        // like the real one, a message id that was sent before is not sent again
        if (sent.stream().noneMatch(earlier -> earlier.id().equals(command.id()))) {
            sent.add(command);
        }
        return null;
    }

    Optional<SendMessageCommand> only() {
        return sent.size() == 1 ? Optional.of(sent.getFirst()) : Optional.empty();
    }

    @Override
    public List<ConversationOverview> listConversations(AccountId person) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int unreadCount(AccountId person) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessagePage messages(AccountId person, AccountId client, MessageId before) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markRead(AccountId person, AccountId client) {
        throw new UnsupportedOperationException();
    }
}
