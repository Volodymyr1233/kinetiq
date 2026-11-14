package com.kinetiq.common.infrastructure.output;

import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.common.domain.output.NoticeBoard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// the notification feature listens in its own input adapter, so the raising features never know it
@Component
@RequiredArgsConstructor
class SpringNoticeBoard implements NoticeBoard {

    private final ApplicationEventPublisher publisher;

    @Override
    public void raise(Notice notice) {
        publisher.publishEvent(notice);
    }
}
