package com.kinetiq.messaging.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.Conversation;
import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.ConversationPair;
import java.time.Instant;
import java.util.Optional;

public interface ConversationRepository {

    Optional<Conversation> find(ConversationPair pair);

    /** Returns the pair's conversation, storing it first if needed; two first sends at once end with one. */
    Conversation findOrCreate(ConversationPair pair);

    /** Sets the last-read time to the later of the stored one and {@code at}. Only the reader's own time changes. */
    void markRead(ConversationId conversation, AccountId reader, Instant at);
}
