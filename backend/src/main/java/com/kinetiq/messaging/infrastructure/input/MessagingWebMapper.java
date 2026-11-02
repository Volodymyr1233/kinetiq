package com.kinetiq.messaging.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import com.kinetiq.messaging.domain.model.LastMessage;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessagePage;
import com.kinetiq.messaging.domain.model.PlanWeek;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface MessagingWebMapper {

    default ConversationListResponse toListResponse(List<ConversationOverview> conversations) {
        return new ConversationListResponse(conversations.stream().map(this::toResponse).toList());
    }

    default ConversationResponse toResponse(ConversationOverview conversation) {
        return new ConversationResponse(
                conversation.getClient().value(),
                conversation.getName(),
                conversation.getGoal().orElse(null),
                conversation.getPlanWeek().map(PlanWeek::current).orElse(null),
                conversation.getPlanWeek().map(PlanWeek::total).orElse(null),
                conversation.getUnread(),
                conversation.getLastMessage().map(this::toResponse).orElse(null));
    }

    default LastMessageResponse toResponse(LastMessage message) {
        return new LastMessageResponse(message.preview(), message.sentByViewer(), message.receivedAt());
    }

    default MessagePageResponse toResponse(MessagePage page, AccountId reader) {
        return new MessagePageResponse(
                page.messages().stream().map(message -> toResponse(message, reader)).toList(), page.hasMore());
    }

    default MessageResponse toResponse(Message message, AccountId reader) {
        return new MessageResponse(
                message.getId().value(),
                message.getText().value(),
                message.getSender().equals(reader),
                message.getReceivedAt());
    }
}
