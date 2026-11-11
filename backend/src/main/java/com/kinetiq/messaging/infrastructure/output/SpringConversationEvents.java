package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.messaging.domain.model.ConversationChange;
import com.kinetiq.messaging.domain.output.ConversationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// the streams listen after commit in the input adapter, so the two adapters share only the domain record
@Component
@RequiredArgsConstructor
class SpringConversationEvents implements ConversationEvents {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(ConversationChange change) {
        publisher.publishEvent(change);
    }
}
