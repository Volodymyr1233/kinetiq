package com.kinetiq.client.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import java.util.List;

public interface ClientDirectory {

    /** The clients linked to the trainer, ordered by name without regard to letter case, then by id. */
    List<ClientSummary> findClientsOf(AccountId trainer);
}
