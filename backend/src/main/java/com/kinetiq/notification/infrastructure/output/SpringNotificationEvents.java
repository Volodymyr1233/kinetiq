package com.kinetiq.notification.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.NoticeStored;
import com.kinetiq.notification.domain.output.NotificationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// the streams listen after commit in messaging's input adapter, so the two features share only the common record
@Component
@RequiredArgsConstructor
class SpringNotificationEvents implements NotificationEvents {

    private final ApplicationEventPublisher publisher;

    @Override
    public void stored(AccountId recipient) {
        publisher.publishEvent(new NoticeStored(recipient.value()));
    }
}
