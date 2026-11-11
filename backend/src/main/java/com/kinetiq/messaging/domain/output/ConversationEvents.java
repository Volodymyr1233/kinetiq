package com.kinetiq.messaging.domain.output;

import com.kinetiq.messaging.domain.model.ConversationChange;

/** Tells a person's open streams that a conversation changed. */
public interface ConversationEvents {

    /**
     * Tells every open stream of {@code change.person()}, once the current transaction has committed. It never fails
     * the caller: a stream that cannot be told is closed.
     */
    void publish(ConversationChange change);
}
