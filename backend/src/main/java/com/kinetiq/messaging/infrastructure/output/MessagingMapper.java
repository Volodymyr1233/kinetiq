package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.Conversation;
import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.ConversationPair;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessageText;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface MessagingMapper {

    default Conversation toDomain(ConversationEntity entity) {
        return new Conversation(
                new ConversationId(entity.getId()),
                new ConversationPair(new AccountId(entity.getClientId()), new AccountId(entity.getTrainerId())));
    }

    default Message toDomain(MessageEntity entity) {
        return new Message(
                new MessageId(entity.getId()),
                new ConversationId(entity.getConversationId()),
                new AccountId(entity.getSenderId()),
                new MessageText(entity.getText()),
                entity.getReceivedAt());
    }
}
