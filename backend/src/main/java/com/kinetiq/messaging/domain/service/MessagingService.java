package com.kinetiq.messaging.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.exception.AboutSetNotFoundException;
import com.kinetiq.messaging.domain.exception.ConversationNotFoundException;
import com.kinetiq.messaging.domain.exception.MessageIdInUseException;
import com.kinetiq.messaging.domain.exception.MessageNotFoundException;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.messaging.domain.input.SendMessageCommand;
import com.kinetiq.messaging.domain.model.AboutSet;
import com.kinetiq.messaging.domain.model.Conversation;
import com.kinetiq.messaging.domain.model.ConversationChange;
import com.kinetiq.messaging.domain.model.ConversationChange.Kind;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import com.kinetiq.messaging.domain.model.ConversationPair;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import com.kinetiq.messaging.domain.model.MessageText;
import com.kinetiq.messaging.domain.output.AboutSets;
import com.kinetiq.messaging.domain.output.ConversationDirectory;
import com.kinetiq.messaging.domain.output.ConversationEvents;
import com.kinetiq.messaging.domain.output.ConversationParticipants;
import com.kinetiq.messaging.domain.output.ConversationRepository;
import com.kinetiq.messaging.domain.output.MessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessagingService implements ExchangeMessagesUseCase {

    private final ConversationParticipants participants;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ConversationDirectory directory;
    private final AboutSets aboutSets;
    private final ConversationEvents events;
    private final Clock clock;

    @Override
    public List<ConversationOverview> listConversations(AccountId person) {
        return directory.overviewOf(person);
    }

    @Override
    public int unreadCount(AccountId person) {
        return directory.unreadTotal(person);
    }

    @Override
    public MessagePage messages(AccountId person, AccountId client, MessageId before) {
        Optional<Conversation> conversation = conversations.find(pairOf(person, client));
        if (before != null) {
            return messages.before(messageIn(conversation, before));
        }
        return conversation.map(found -> messages.newest(found.getId())).orElseGet(MessagePage::empty);
    }

    // the text and the set come first, so a refused message leaves no conversation behind
    @Override
    public Message send(SendMessageCommand command) {
        ConversationPair pair = pairOf(command.person(), command.client());
        MessageText text = new MessageText(command.text());
        AboutSet about = aboutOf(command);
        Conversation conversation = conversations.findOrCreate(pair);
        Message message = new Message(command.id(), conversation.getId(), command.person(), text, now(), about);
        if (messages.saveIfNew(message)) {
            // the sender too, so their other tabs and devices show it
            events.publish(new ConversationChange(pair.client(), pair.client(), Kind.MESSAGE));
            events.publish(new ConversationChange(pair.trainer(), pair.client(), Kind.MESSAGE));
            return message;
        }
        Message stored = messages.findById(command.id()).orElseThrow(MessageIdInUseException::new);
        if (!stored.isSameSendAs(message)) {
            throw new MessageIdInUseException();
        }
        return stored;
    }

    @Override
    public void markRead(AccountId person, AccountId client) {
        conversations.find(pairOf(person, client)).ifPresent(conversation -> {
            conversations.markRead(conversation.getId(), person, now());
            // only the reader: the other person is never told, there are no read receipts
            events.publish(new ConversationChange(person, client, Kind.READ));
        });
    }

    // the one access rule: every operation on a conversation starts here
    private ConversationPair pairOf(AccountId person, AccountId client) {
        return participants
                .pairOf(client)
                .filter(pair -> pair.includes(person))
                .orElseThrow(ConversationNotFoundException::new);
    }

    // only the sender's own set: anyone else's, an unknown one and any set named by a trainer answer the same
    private AboutSet aboutOf(SendMessageCommand command) {
        if (command.about() == null) {
            return null;
        }
        return aboutSets.ofSender(command.person(), command.about()).orElseThrow(AboutSetNotFoundException::new);
    }

    private Message messageIn(Optional<Conversation> conversation, MessageId id) {
        return conversation
                .flatMap(found -> messages.findById(id).filter(message -> message.getConversation().equals(found.getId())))
                .orElseThrow(MessageNotFoundException::new);
    }

    // PostgreSQL keeps microseconds, so a time read back equals the time written and "after my last-read time" is exact
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
