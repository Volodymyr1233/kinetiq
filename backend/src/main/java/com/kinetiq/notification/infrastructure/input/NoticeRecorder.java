package com.kinetiq.notification.infrastructure.input;

import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.notification.domain.input.RecordNoticeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// in the transaction of the change that raised the notice, so the notification exists only when the change does
@Component
@RequiredArgsConstructor
class NoticeRecorder {

    private final RecordNoticeUseCase notices;

    @EventListener
    void on(Notice notice) {
        notices.record(notice);
    }
}
