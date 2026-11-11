package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.infrastructure.config.TransactionConfig;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.input.DecideOwnPlanChangeUseCase;
import com.kinetiq.planchange.domain.input.DecidePlanChangeUseCase;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import com.kinetiq.planchange.domain.service.OwnPlanChangeDecisionService;
import com.kinetiq.planchange.domain.service.PlanChangeDecisionService;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two people accept at the same moment: neither acceptance may be lost. The test has no transaction of its own, so
 * each use case call commits like a request, and each thread sees the other's committed work.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    TestcontainersConfiguration.class,
    PlanChangeRepositoryImpl.class,
    PlanChangeMapperImpl.class,
    TransactionConfig.class,
    PlanChangeConcurrencyIT.Wiring.class
})
class PlanChangeConcurrencyIT {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Autowired
    private DecidePlanChangeUseCase trainerDecisions;

    @Autowired
    private DecideOwnPlanChangeUseCase clientDecisions;

    @Autowired
    private PlanChangeRepository planChanges;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<UUID> trainers = new ArrayList<>();
    private final List<UUID> clients = new ArrayList<>();
    private UUID exercise;

    @AfterEach
    void cleanUp() {
        for (UUID client : clients) {
            for (String child : List.of("plan_change_targets", "plan_change_exercises")) {
                jdbc.update(
                        "delete from " + child + " where plan_change_id in (select id from plan_changes where client_id = ?)",
                        client);
            }
            jdbc.update("delete from plan_changes where client_id = ?", client);
            jdbc.update("delete from accounts where id = ?", client);
        }
        trainers.forEach(trainer -> jdbc.update("delete from accounts where id = ?", trainer));
        if (exercise != null) {
            jdbc.update("delete from exercises where id = ?", exercise);
        }
    }

    private UUID insertAccount(String role, UUID trainer) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Account', ?, 'hash', ?, ?, 'Europe/Warsaw', true,
                        case when ? = 'CLIENT' then now() end, now(), now(), 0)
                """,
                id,
                id + "@example.com",
                role,
                trainer,
                role);
        return id;
    }

    @RepeatedTest(10)
    void shouldEndApplied_withBothAcceptances_whenTheTrainerAndTheClientAcceptAtOnce() throws Exception {
        UUID trainer = insertAccount("TRAINER", null);
        trainers.add(trainer);
        UUID client = insertAccount("CLIENT", trainer);
        clients.add(client);
        exercise = UUID.randomUUID();
        jdbc.update(
                """
                insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount,
                                       rest_seconds, cues, created_at, updated_at, version)
                values (?, null, ?, 'LEGS', '', 'REPS', 3, 10, 90, '', now(), now(), 0)
                """,
                exercise,
                "Exercise " + exercise);
        PlanChange stored = planChanges.save(new PlanChange(
                PlanChangeId.random(),
                new AccountId(client),
                Rule.HIGH_RPE,
                PlanChangeState.PENDING,
                1,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                LocalDate.parse("2026-11-16"),
                PlanChangeContent.ofChanges(List.of(new TargetChange(
                        new EntryRef(DayOfWeek.MONDAY, 0),
                        new ExerciseId(exercise),
                        "Lower body",
                        Target.AMOUNT,
                        new BigDecimal("10.0"),
                        new BigDecimal("9.0")))),
                List.of(),
                Set.of(),
                NOW.minusSeconds(3600),
                null,
                null,
                null,
                null));

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> byTrainer = threads.submit(() -> {
                start.await();
                return trainerDecisions.accept(new AccountId(trainer), stored.getId());
            });
            Future<?> byClient = threads.submit(() -> {
                start.await();
                return clientDecisions.accept(new AccountId(client), stored.getId());
            });
            start.countDown();
            byTrainer.get();
            byClient.get();
        }

        // read with SQL: the repository needs the transaction of a use case to read the content
        Map<String, Object> row = jdbc.queryForMap("select * from plan_changes where id = ?", stored.getId().value());
        assertThat(row).containsEntry("state", "APPLIED");
        assertThat(row.get("trainer_accepted_at")).isNotNull();
        assertThat(row.get("client_accepted_at")).isNotNull();
        assertThat(row.get("ended_at")).isNotNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Wiring {

        // the clients and accounts of other features are not part of this test: one trainer owns every client
        @Bean
        ViewClientsUseCase viewClientsUseCase() {
            return new ViewClientsUseCase() {
                @Override
                public List<ClientSummary> listClients(AccountId trainer) {
                    return List.of();
                }

                @Override
                public ClientSummary getClient(AccountId trainer, AccountId client) {
                    return new ClientSummary(client, "Client", Goal.LOSE_FAT, PlanState.PUBLISHED, WARSAW);
                }
            };
        }

        @Bean
        CurrentAccountUseCase currentAccountUseCase() {
            return new CurrentAccountUseCase() {
                @Override
                public Account find(AccountId id) {
                    return Account.restore(
                            id,
                            "Person",
                            new Email("person@example.com"),
                            "hash",
                            Role.CLIENT,
                            AccountId.random(),
                            WARSAW,
                            AccountSettings.startingWith(Language.EN),
                            true,
                            null,
                            null,
                            null,
                            null);
                }

                @Override
                public void changeTimeZone(AccountId id, ZoneId zone) {}

                @Override
                public Account update(AccountId id, UpdateAccountCommand command) {
                    return find(id);
                }
            };
        }

        @Bean
        PlanSource planSource(JdbcTemplate jdbc) {
            return new JdbcPlanSource(jdbc);
        }

        @Bean
        DecidePlanChangeUseCase decidePlanChangeUseCase(
                ViewClientsUseCase clients,
                CurrentAccountUseCase accounts,
                PlanChangeRepository planChanges,
                PlanSource plans) {
            return new PlanChangeDecisionService(
                    clients, accounts, planChanges, plans, mock(ExchangeMessagesUseCase.class), Clock.fixed(NOW, ZoneOffset.UTC));
        }

        @Bean
        DecideOwnPlanChangeUseCase decideOwnPlanChangeUseCase(
                CurrentAccountUseCase accounts, PlanChangeRepository planChanges, PlanSource plans) {
            return new OwnPlanChangeDecisionService(accounts, planChanges, plans, mock(ExchangeMessagesUseCase.class), Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }
}
