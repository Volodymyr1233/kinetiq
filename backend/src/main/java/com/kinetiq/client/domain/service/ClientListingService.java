package com.kinetiq.client.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ListClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.output.ClientDirectory;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientListingService implements ListClientsUseCase {

    private final ClientDirectory clients;

    @Override
    public List<ClientSummary> listClients(AccountId trainer) {
        return clients.findClientsOf(trainer);
    }
}
