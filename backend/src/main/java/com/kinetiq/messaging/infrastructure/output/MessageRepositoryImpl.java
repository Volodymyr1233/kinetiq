package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import com.kinetiq.messaging.domain.output.MessageRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class MessageRepositoryImpl implements MessageRepository {

    // one more than a page, to know whether earlier messages exist
    private static final Limit PAGE_AND_ONE = Limit.of(MessagePage.SIZE + 1);

    private final JpaMessageRepository jpa;
    private final MessagingMapper mapper;

    @Override
    public Optional<Message> findById(MessageId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean saveIfNew(Message message) {
        return jpa.insertIfAbsent(
                        message.getId().value(),
                        message.getConversation().value(),
                        message.getSender().value(),
                        message.getText().value(),
                        message.getReceivedAt())
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
                newestFirst.stream().limit(MessagePage.SIZE).map(mapper::toDomain).toList().reversed(),
                newestFirst.size() > MessagePage.SIZE);
    }
}
