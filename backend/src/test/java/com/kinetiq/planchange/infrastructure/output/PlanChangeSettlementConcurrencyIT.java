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
import com.kinetiq.plan.domain.input.RevisePlanUseCase;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.planchange.domain.input.ApplyPlanChangesUseCase;
import com.kinetiq.planchange.domain.input.DecideOwnPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import com.kinetiq.planchange.domain.service.OwnPlanChangeDecisionService;
import com.kinetiq.planchange.domain.service.PlanChangeSettlementService;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
 * The second person accepts at the very moment the effective Monday settles the plan change: the plan change ends in
 * exactly one way. The two use cases get clocks on either side of the Monday, as the two threads would see in
 * production when the boundary passes between them. The test has no transaction of its own, so each call commits like
 * a request.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    TestcontainersConfiguration.class,
    PlanChangeRepositoryImpl.class,
    PlanChangeMapperImpl.class,
    TransactionConfig.class,
    PlanChangeSettlementConcurrencyIT.Wiring.class
})
class PlanChangeSettlementConcurrencyIT {

    private static final Instant BEFORE_MONDAY = Instant.parse("2026-11-15T22:50:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-15T23:10:00Z");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final List<Session> REVISIONS = new CopyOnWriteArrayList<>();

    @Autowired
    private DecideOwnPlanChangeUseCase clientDecisions;

    @Autowired
    private ApplyPlanChangesUseCase settlement;

    @Autowired
    private PlanChangeRepository planChanges;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<UUID> trainers = new ArrayList<>();
    private final List<UUID> clients = new ArrayList<>();
    private UUID exercise;

    @AfterEach
    void cleanUp() {
        REVISIONS.clear();
        clients.forEach(client -> jdbc.update("delete from accounts where id = ?", client));
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

    // a published plan of two sessions, whose Monday squat the plan change lowers from 10 to 9 repetitions
    private void insertPlan(UUID client) {
        UUID plan = UUID.randomUUID();
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at, version)
                values (?, ?, 'BUILD_MUSCLE', 12, date '2026-09-07', date '2026-09-07', now(), now(), 3)
                """,
                plan,
                client);
        for (String day : List.of("MONDAY", "THURSDAY")) {
            UUID session = UUID.randomUUID();
            jdbc.update(
                    "insert into plan_sessions (id, plan_id, name, day_of_week) values (?, ?, ?, ?)",
                    session,
                    plan,
                    "Session " + day,
                    day);
            jdbc.update(
                    """
                    insert into plan_session_entries (session_id, position, exercise_id, sets, amount, load_kg, rpe,
                                                      rest_seconds)
                    values (?, 0, ?, 3, 10, 60.0, 8, 90)
                    """,
                    session,
                    exercise);
        }
    }

    @RepeatedTest(10)
    void shouldEndInExactlyOneWay_whenTheSecondAcceptanceRacesTheEffectiveMonday() throws Exception {
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
        insertPlan(client);
        PlanChange pending = new PlanChange(
                PlanChangeId.random(),
                new AccountId(client),
                Rule.HIGH_RPE,
                PlanChangeState.PENDING,
                3,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                LocalDate.parse("2026-11-16"),
                PlanChangeContent.ofChanges(List.of(new TargetChange(
                        new EntryRef(DayOfWeek.MONDAY, 0),
                        new com.kinetiq.exercise.domain.model.ExerciseId(exercise),
                        "Session MONDAY",
                        Target.AMOUNT,
                        new BigDecimal("10.0"),
                        new BigDecimal("9.0")))),
                List.of(),
                Set.of(),
                BEFORE_MONDAY.minusSeconds(86_400),
                null,
                null,
                null,
                null);
        PlanChange stored = planChanges.save(
                pending.accept(Side.TRAINER, BEFORE_MONDAY.minusSeconds(3600), LocalDate.parse("2026-11-15")));

        CountDownLatch start = new CountDownLatch(1);
        boolean acceptanceRefused;
        try (ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> accepting = threads.submit(() -> {
                start.await();
                return clientDecisions.accept(new AccountId(client), stored.getId());
            });
            Future<?> settling = threads.submit(() -> {
                start.await();
                settlement.settle(stored.getId());
                return null;
            });
            start.countDown();
            settling.get();
            try {
                accepting.get();
                acceptanceRefused = false;
            } catch (ExecutionException refused) {
                acceptanceRefused = true;
            }
        }

        // read with SQL: the repository needs the transaction of a use case to read the content
        Map<String, Object> row = jdbc.queryForMap("select * from plan_changes where id = ?", stored.getId().value());
        if (acceptanceRefused) {
            assertThat(row).containsEntry("state", "EXPIRED").containsEntry("expiry_cause", "UNDECIDED");
            assertThat(row.get("took_effect_at")).isNull();
            assertThat(REVISIONS).isEmpty();
        } else {
            assertThat(row).containsEntry("state", "APPLIED");
            assertThat(row.get("took_effect_at")).isNotNull();
            assertThat(row.get("expiry_cause")).isNull();
            assertThat(REVISIONS).hasSize(2);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Wiring {

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

        // the plan feature is not part of this test: the revision only records the sessions it was asked to store
        @Bean
        RevisePlanUseCase revisePlanUseCase() {
            return (client, expectedVersion, effectiveWeek, sessions) -> REVISIONS.addAll(sessions);
        }

        @Bean
        DecideOwnPlanChangeUseCase decideOwnPlanChangeUseCase(
                CurrentAccountUseCase accounts, PlanChangeRepository planChanges, PlanSource plans) {
            return new OwnPlanChangeDecisionService(
                    accounts,
                    planChanges,
                    plans,
                    mock(ExchangeMessagesUseCase.class),
                    notice -> {},
                    Clock.fixed(BEFORE_MONDAY, ZoneOffset.UTC));
        }

        @Bean
        ApplyPlanChangesUseCase applyPlanChangesUseCase(
                CurrentAccountUseCase accounts,
                PlanChangeRepository planChanges,
                PlanSource plans,
                RevisePlanUseCase revisions) {
            return new PlanChangeSettlementService(
                    accounts, planChanges, plans, revisions, Clock.fixed(MONDAY, ZoneOffset.UTC));
        }
    }
}
