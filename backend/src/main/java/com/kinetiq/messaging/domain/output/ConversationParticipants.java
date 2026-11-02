package com.kinetiq.messaging.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.ConversationPair;
import java.util.Optional;

/** Who may use a client's conversation. */
public interface ConversationParticipants {

    /** Returns the client with their current trainer; empty when the id is not a client. */
    Optional<ConversationPair> pairOf(AccountId client);
}
