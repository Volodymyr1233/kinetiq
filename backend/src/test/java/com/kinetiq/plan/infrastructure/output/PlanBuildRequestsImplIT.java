package com.kinetiq.plan.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.plan.domain.exception.PlanDraftingInProgressException;
import com.kinetiq.plan.domain.exception.PlanRebuildLimitReachedException;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.service.PlanRebuildService;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({
    TestcontainersConfiguration.class,
    PlanBuildRequestsImpl.class,
    PlanDraftingRepositoryImpl.class,
    PlanRepositoryImpl.class,
    PlanMapperImpl.class
})
class PlanBuildRequestsImplIT {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    @Autowired
    private PlanBuildRequestsImpl requests;

    @Autowired
    private PlanDraftingRepositoryImpl draftings;

    @Autowired
    private PlanRepositoryImpl plans;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactions;

    private AccountId trainer;

    @BeforeEach
    void insertTrainer() {
        trainer = insertAccount("TRAINER", null);
    }

    private AccountId insertAccount(String role, AccountId trainerOf) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Name', ?, 'hash', ?, ?, 'Europe/Warsaw', true,
                        case when ? = 'CLIENT' then now() end, now(), now(), 0)
                """,
                id,
                id + "@example.com",
                role,
                trainerOf == null ? null : trainerOf.value(),
                role);
        return new AccountId(id);
    }

    private int countOf(AccountId owner) {
        return jdbc.queryForObject(
                "select count(*) from plan_build_requests where trainer_id = ?", Integer.class, owner.value());
    }

    @Test
    void shouldListOnlyTheTrainersRequestsAfterTheInstant_oldestFirst() {
        AccountId other = insertAccount("TRAINER", null);
        requests.record(trainer, NOW.minusSeconds(10), Instant.EPOCH);
        requests.record(trainer, NOW.minusSeconds(30), Instant.EPOCH);
        requests.record(trainer, NOW.minusSeconds(60), Instant.EPOCH);
        requests.record(other, NOW.minusSeconds(20), Instant.EPOCH);

        assertThat(requests.since(trainer, NOW.minusSeconds(60)))
                .containsExactly(NOW.minusSeconds(30), NOW.minusSeconds(10));
    }

    @Test
    void shouldForgetTheTrainersOlderRequests_whenRecording() {
        AccountId other = insertAccount("TRAINER", null);
        requests.record(trainer, NOW.minus(Duration.ofHours(30)), Instant.EPOCH);
        requests.record(other, NOW.minus(Duration.ofHours(30)), Instant.EPOCH);

        requests.record(trainer, NOW, NOW.minus(Duration.ofHours(24)));

        assertThat(requests.since(trainer, Instant.EPOCH)).containsExactly(NOW);
        assertThat(countOf(other)).isEqualTo(1);
    }

    // committed for real, because the point is transactions meeting in the database
    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class InParallel {

        // every client is the trainer's own
        private final ViewClientsUseCase clients = new ViewClientsUseCase() {
            @Override
            public List<ClientSummary> listClients(AccountId owner) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ClientSummary getClient(AccountId owner, AccountId client) {
                return new ClientSummary(client, "Name", Goal.LOSE_FAT, PlanState.DRAFT, ZoneId.of("Europe/Warsaw"));
            }
        };

        private final PlanRebuildService service = new PlanRebuildService(
                clients, plans, draftings, requests, Clock.fixed(NOW, ZoneOffset.UTC));

        private AccountId clientWithDraft() {
            AccountId client = insertAccount("CLIENT", trainer);
            new TransactionTemplate(transactions)
                    .executeWithoutResult(status -> plans.save(Plan.draft(client, Goal.LOSE_FAT, 12, List.of())));
            return client;
        }

        // each attempt in its own transaction, as one use case call is, all released at once
        private List<Throwable> allAtOnce(List<AccountId> clients) throws InterruptedException {
            TransactionTemplate transaction = new TransactionTemplate(transactions);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> attempts = new ArrayList<>();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (AccountId client : clients) {
                    Callable<Void> attempt = () -> {
                        start.await();
                        transaction.executeWithoutResult(status -> service.rebuild(trainer, client));
                        return null;
                    };
                    attempts.add(executor.submit(attempt));
                }
                start.countDown();
            }
            List<Throwable> refusals = new ArrayList<>();
            for (Future<?> attempt : attempts) {
                try {
                    attempt.get();
                } catch (ExecutionException refused) {
                    refusals.add(refused.getCause());
                }
            }
            return refusals;
        }

        @Test
        void shouldAcceptExactlyTen_whenTwelveRequestsForDifferentClientsMeet() throws InterruptedException {
            List<AccountId> clients = new ArrayList<>();
            for (int count = 0; count < 12; count++) {
                clients.add(clientWithDraft());
            }

            List<Throwable> refusals = allAtOnce(clients);

            assertThat(refusals).hasSize(2).allMatch(PlanRebuildLimitReachedException.class::isInstance);
            assertThat(countOf(trainer)).isEqualTo(10);
        }

        @Test
        void shouldStartOneDraftingAndCountOne_whenTwoRequestsForOneClientMeet() throws InterruptedException {
            AccountId client = clientWithDraft();

            List<Throwable> refusals = allAtOnce(List.of(client, client));

            assertThat(refusals).singleElement().isInstanceOf(PlanDraftingInProgressException.class);
            assertThat(draftings.exists(client)).isTrue();
            assertThat(countOf(trainer)).isEqualTo(1);
        }
    }
}
