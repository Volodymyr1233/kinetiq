package com.kinetiq.messaging.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import java.util.List;

/**
 * What a person's conversations look like from their side. A message is unread when the other person sent it and it
 * was received after the reader's last-read time; every message from the other person is unread before the first read.
 */
public interface ConversationDirectory {

    /**
     * Returns a trainer's row for every linked client, or a client's one row with their trainer. Rows with a last
     * message come first, newest first, then the others by name.
     */
    List<ConversationOverview> overviewOf(AccountId person);

    /** Returns the sum of the unread counts of {@link #overviewOf}. */
    int unreadTotal(AccountId person);
}
