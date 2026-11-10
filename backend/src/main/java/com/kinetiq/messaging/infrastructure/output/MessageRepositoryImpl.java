package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.messaging.domain.model.AboutSet;
import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import com.kinetiq.messaging.domain.output.MessageRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class MessageRepositoryImpl implements MessageRepository {

    // one more than a page, to know whether earlier messages exist
    private static final Limit PAGE_AND_ONE = Limit.of(MessagePage.SIZE + 1);

    private final JpaMessageRepository jpa;
    private final JdbcAboutSets aboutSets;
    private final MessagingMapper mapper;

    @Override
    public Optional<Message> findById(MessageId id) {
        return jpa.findById(id.value()).map(entity -> withSets(List.of(entity)).getFirst());
    }

    @Override
    public boolean saveIfNew(Message message) {
        return jpa.insertIfAbsent(
                        message.getId().value(),
                        message.getConversation().value(),
                        message.getSender().value(),
                        message.getText().value(),
                        message.getReceivedAt(),
                        message.getAbout().map(about -> about.setLog().value().toString()).orElse(null))
                == 1;
    }

    @Override
    public MessagePage newest(ConversationId conversation) {
        return page(jpa.findNewest(conversation.value(), PAGE_AND_ONE));
    }

    @Override
    public MessagePage before(Message message) {
        return page(jpa.findBefore(
                message.getConversation().value(), message.getReceivedAt(), message.getId().value(), PAGE_AND_ONE));
    }

    private MessagePage page(List<MessageEntity> newestFirst) {
        return new MessagePage(
                withSets(newestFirst.stream().limit(MessagePage.SIZE).toList()).reversed(),
                newestFirst.size() > MessagePage.SIZE);
    }

    // one more query for all the sets of a page, and none when no message of it is about a set
    private List<Message> withSets(List<MessageEntity> entities) {
        Map<UUID, AboutSet> sets = aboutSets.byIds(entities.stream()
                .map(MessageEntity::getAboutSetLogId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return entities.stream()
                .map(entity -> mapper.toDomain(
                        entity, entity.getAboutSetLogId() == null ? null : sets.get(entity.getAboutSetLogId())))
                .toList();
    }
}
