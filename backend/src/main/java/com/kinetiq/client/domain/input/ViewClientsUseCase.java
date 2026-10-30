package com.kinetiq.client.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import java.util.List;

public interface ViewClientsUseCase {

    /** The trainer's own clients, ordered by name without regard to letter case; nobody else's. */
    List<ClientSummary> listClients(AccountId trainer);

    /** One of the trainer's own clients. Refused with {@code ClientNotFoundException} for any other account. */
    ClientSummary getClient(AccountId trainer, AccountId client);
}
