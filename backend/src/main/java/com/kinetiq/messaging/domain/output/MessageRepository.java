package com.kinetiq.messaging.domain.output;

import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import java.util.Optional;

/** Pages order messages by the time the server received them, then by id, and run oldest to newest. */
public interface MessageRepository {

    Optional<Message> findById(MessageId id);

    /**
     * Stores the message unless its id exists already, which also settles two attempts at once.
     *
     * @return whether it was stored
     */
    boolean saveIfNew(Message message);

    /** Returns the newest {@link MessagePage#SIZE} messages of a conversation. */
    MessagePage newest(ConversationId conversation);

    /** Returns the {@link MessagePage#SIZE} messages right before {@code message} in its conversation. */
    MessagePage before(Message message);
}
