package com.kinetiq.messaging.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.messaging.domain.model.AboutSet;
import com.kinetiq.messaging.domain.model.Conversation;
import com.kinetiq.messaging.domain.model.ConversationChange;
import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import com.kinetiq.messaging.domain.model.ConversationPair;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import com.kinetiq.messaging.domain.output.AboutSets;
import com.kinetiq.messaging.domain.output.ConversationDirectory;
import com.kinetiq.messaging.domain.output.ConversationEvents;
import com.kinetiq.messaging.domain.output.ConversationParticipants;
import com.kinetiq.messaging.domain.output.ConversationRepository;
import com.kinetiq.messaging.domain.output.MessageRepository;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class MessagingFakes {

    private MessagingFakes() {}

    static class Participants implements ConversationParticipants {

        private final Map<AccountId, AccountId> trainerOf = new HashMap<>();

        void link(AccountId client, AccountId trainer) {
            trainerOf.put(client, trainer);
        }

        @Override
        public Optional<ConversationPair> pairOf(AccountId client) {
            return Optional.ofNullable(trainerOf.get(client)).map(trainer -> new ConversationPair(client, trainer));
        }
    }

    static class Conversations implements ConversationRepository {

        final Map<ConversationPair, Conversation> stored = new HashMap<>();
        final Map<ConversationId, Map<AccountId, Instant>> reads = new HashMap<>();

        @Override
        public Optional<Conversation> find(ConversationPair pair) {
            return Optional.ofNullable(stored.get(pair));
        }

        @Override
        public Conversation findOrCreate(ConversationPair pair) {
            return stored.computeIfAbsent(pair, key -> new Conversation(ConversationId.random(), key));
        }

        @Override
        public void markRead(ConversationId conversation, AccountId reader, Instant at) {
            reads.computeIfAbsent(conversation, key -> new HashMap<>()).put(reader, at);
        }
    }

    static class Messages implements MessageRepository {

        final Map<MessageId, Message> stored = new LinkedHashMap<>();

        @Override
        public Optional<Message> findById(MessageId id) {
            return Optional.ofNullable(stored.get(id));
        }

        @Override
        public boolean saveIfNew(Message message) {
            return stored.putIfAbsent(message.getId(), message) == null;
        }

        @Override
        public MessagePage newest(ConversationId conversation) {
            return new MessagePage(ofConversation(conversation), false);
        }

        @Override
        public MessagePage before(Message message) {
            return new MessagePage(
                    ofConversation(message.getConversation()).stream()
                            .filter(other -> other.getReceivedAt().isBefore(message.getReceivedAt()))
                            .toList(),
                    false);
        }

        private List<Message> ofConversation(ConversationId conversation) {
            return new ArrayList<>(stored.values().stream()
                    .filter(message -> message.getConversation().equals(conversation))
                    .sorted(Comparator.comparing(Message::getReceivedAt))
                    .toList());
        }
    }

    static class Sets implements AboutSets {

        private final Map<SetLogId, AccountId> clientOf = new HashMap<>();
        private final Map<SetLogId, AboutSet> logged = new HashMap<>();

        AboutSet logFor(AccountId client) {
            AboutSet set = new AboutSet(
                    new SetLogId(UUID.randomUUID()),
                    SessionLogId.random(),
                    "Lower body",
                    LocalDate.of(2026, 11, 2),
                    new LocalizedText("Back squat", "Przysiad ze sztangą"),
                    0,
                    3,
                    9);
            clientOf.put(set.setLog(), client);
            logged.put(set.setLog(), set);
            return set;
        }

        @Override
        public Optional<AboutSet> ofSender(AccountId sender, SetLogId setLog) {
            return Optional.ofNullable(logged.get(setLog)).filter(set -> sender.equals(clientOf.get(setLog)));
        }
    }

    static class Directory implements ConversationDirectory {

        @Override
        public List<ConversationOverview> overviewOf(AccountId person) {
            return List.of();
        }

        @Override
        public int unreadTotal(AccountId person) {
            return 0;
        }
    }

    static class Events implements ConversationEvents {

        final List<ConversationChange> published = new ArrayList<>();

        @Override
        public void publish(ConversationChange change) {
            published.add(change);
        }
    }
}
