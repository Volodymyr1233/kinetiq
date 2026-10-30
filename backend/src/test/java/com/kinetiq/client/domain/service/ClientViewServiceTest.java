package com.kinetiq.client.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.client.domain.output.ClientDirectory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClientViewServiceTest {

    private static final AccountId MAREK = AccountId.random();
    private static final AccountId OLA_TRAINER = AccountId.random();
    private final ClientSummary anna = new ClientSummary(AccountId.random(), "Anna", null, PlanState.NO_QUIZ);

    // a directory that knows which trainer each client belongs to
    private final ClientDirectory directory = new ClientDirectory() {
        private final Map<AccountId, ClientSummary> clientsOfMarek = Map.of(anna.getId(), anna);

        @Override
        public List<ClientSummary> findClientsOf(AccountId trainer) {
            return MAREK.equals(trainer) ? List.copyOf(clientsOfMarek.values()) : List.of();
        }

        @Override
        public Optional<ClientSummary> findClient(AccountId trainer, AccountId client) {
            return MAREK.equals(trainer) ? Optional.ofNullable(clientsOfMarek.get(client)) : Optional.empty();
        }
    };

    private final ClientViewService service = new ClientViewService(directory);

    @Test
    void shouldReturnTheClient_whenItIsTheTrainersOwn() {
        assertThat(service.getClient(MAREK, anna.getId())).isSameAs(anna);
    }

    @Test
    void shouldRefuseAsNotFound_whenTheClientIsAnotherTrainersOrUnknown() {
        assertThatThrownBy(() -> service.getClient(OLA_TRAINER, anna.getId()))
                .isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> service.getClient(MAREK, AccountId.random()))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void shouldListOnlyWhatTheDirectoryHoldsForTheTrainer() {
        assertThat(service.listClients(MAREK)).containsExactly(anna);
        assertThat(service.listClients(OLA_TRAINER)).isEmpty();
    }
}
