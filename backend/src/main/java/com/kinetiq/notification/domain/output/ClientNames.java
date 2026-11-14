package com.kinetiq.notification.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import java.util.Collection;
import java.util.Map;

/** The current names of clients; a notification keeps none of its own, so a deleted client leaves nothing. */
public interface ClientNames {

    /** Names by id; an id that is no account is left out. */
    Map<AccountId, String> namesOf(Collection<AccountId> ids);
}
