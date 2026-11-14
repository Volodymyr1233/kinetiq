package com.kinetiq.notification.domain.input;

import com.kinetiq.common.domain.model.Notice;

/** Stores what a feature says a person is to be told. */
public interface RecordNoticeUseCase {

    /** Stores a notification for the notice's recipient and tells their open streams once the transaction commits. */
    void record(Notice notice);
}
