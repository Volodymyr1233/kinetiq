package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.Conversation;
import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.ConversationPair;
import com.kinetiq.messaging.domain.output.ConversationRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ConversationRepositoryImpl implements ConversationRepository {

    private final JpaConversationRepository jpa;
    private final MessagingMapper mapper;

    @Override
    public Optional<Conversation> find(ConversationPair pair) {
        return jpa.findByClientIdAndTrainerId(pair.client().value(), pair.trainer().value())
                .map(mapper::toDomain);
    }

    @Override
    public Conversation findOrCreate(ConversationPair pair) {
        return find(pair).orElseGet(() -> {
            jpa.insertIfAbsent(ConversationId.random().value(), pair.client().value(), pair.trainer().value());
            return find(pair).orElseThrow();
        });
    }

    @Override
    public void markRead(ConversationId conversation, AccountId reader, Instant at) {
        jpa.markRead(conversation.value(), reader.value(), at);
    }
}
