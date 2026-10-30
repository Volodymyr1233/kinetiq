package com.kinetiq.client.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import java.util.List;

public interface ListClientsUseCase {

    /** The trainer's own clients, ordered by name without regard to letter case; nobody else's. */
    List<ClientSummary> listClients(AccountId trainer);
}
