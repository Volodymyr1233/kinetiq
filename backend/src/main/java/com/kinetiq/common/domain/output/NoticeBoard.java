package com.kinetiq.common.domain.output;

import com.kinetiq.common.domain.model.Notice;

/** Where a feature says that a person is to be told something; the notification feature stores it. */
public interface NoticeBoard {

    /** Raises the notice within the current transaction, so it exists only when the change that caused it does. */
    void raise(Notice notice);
}
