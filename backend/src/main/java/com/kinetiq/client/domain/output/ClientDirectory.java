package com.kinetiq.client.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import java.util.List;
import java.util.Optional;

public interface ClientDirectory {

    /** The clients linked to the trainer, ordered by name without regard to letter case, then by id. */
    List<ClientSummary> findClientsOf(AccountId trainer);

    /** The client, only when the trainer is the one they are linked to. */
    Optional<ClientSummary> findClient(AccountId trainer, AccountId client);
}
