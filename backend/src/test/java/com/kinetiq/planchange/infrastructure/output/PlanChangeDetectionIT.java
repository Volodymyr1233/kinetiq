package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.TransactionConfig;
import com.kinetiq.planchange.domain.service.TrainerOfEveryClient;
import com.kinetiq.planchange.domain.exception.PlanChangeDraftingFailedException;
import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.RemovedSession;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import com.kinetiq.planchange.domain.rule.HighRpeFallback;
import com.kinetiq.planchange.domain.rule.HighRpeLimits;
import com.kinetiq.planchange.domain.rule.HighRpeRule;
import com.kinetiq.planchange.domain.rule.LowCompletionFallback;
import com.kinetiq.planchange.domain.rule.LowCompletionLimits;
import com.kinetiq.planchange.domain.rule.LowCompletionRule;
import com.kinetiq.planchange.domain.rule.StalledWeightFallback;
import com.kinetiq.planchange.domain.rule.StalledWeightLimits;
import com.kinetiq.planchange.domain.rule.StalledWeightRule;
import com.kinetiq.planchange.domain.service.PlanChangeDetectionService;
import com.kinetiq.planchange.domain.service.PlanChangeDraftingService;
import com.kinetiq.planchange.domain.service.PlanChangeLimits;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The weekly check end to end on the real adapters, with a drafting service that always fails: a client who finished 1
 * of 3 planned sessions in each of two weeks gets one pending low completion plan change with its fixed content.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // the use case must open the transaction, not the test
@Import({
    TestcontainersConfiguration.class,
    JdbcTrainingHistory.class,
    JdbcPlanSource.class,
    PlanChangeRepositoryImpl.class,
    PlanChangeMapperImpl.class,
    TransactionConfig.class,
    PlanChangeDetectionIT.Wiring.class
})
class PlanChangeDetectionIT {

    // Monday 2026-11-09, 11:00 in Warsaw: the weeks of 2026-10-26 and 2026-11-02 are complete
    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    @Autowired
    private DetectPlanChangesUseCase detection;

    @Autowired
    private PlanChangeRepository planChanges;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactions;

    private AccountId insertClientWithPlan() {
        UUID trainer = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, time_zone, email_confirmed, created_at,
                                      updated_at, version)
                values (?, 'Trainer', ?, 'hash', 'TRAINER', 'Europe/Warsaw', true, now(), now(), 0)
                """,
                trainer,
                trainer + "@example.com");
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Client', ?, 'hash', 'CLIENT', ?, 'Europe/Warsaw', true, now(), now(), now(), 0)
                """,
                client,
                client + "@example.com",
                trainer);
        UUID exercise = UUID.randomUUID();
        jdbc.update(
                """
                insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount,
                                       rest_seconds, cues, created_at, updated_at, version)
                values (?, null, ?, 'LEGS', '', 'REPS', 3, 10, 90, '', now(), now(), 0)
                """,
                exercise,
                "Exercise " + exercise);
        UUID plan = UUID.randomUUID();
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at, version)
                values (?, ?, 'LOSE_FAT', 8, date '2026-10-05', date '2026-10-05', now(), now(), 2)
                """,
                plan,
                client);
        for (String day : List.of("MONDAY", "WEDNESDAY", "FRIDAY")) {
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
                    values (?, 0, ?, 3, 8, 100.0, 8, 120)
                    """,
                    session,
                    exercise);
        }
        for (String monday : List.of("2026-10-26", "2026-11-02")) {
            jdbc.update(
                    """
                    insert into session_logs (id, client_id, log_date, session_name, started_at, finished_at,
                                              created_at, updated_at, version)
                    values (?, ?, ?, 'Session MONDAY', now(), now(), now(), now(), 0)
                    """,
                    UUID.randomUUID(),
                    client,
                    LocalDate.parse(monday));
        }
        return new AccountId(client);
    }

    @Test
    void shouldStoreOnePendingPlanChangeWithTheFixedContent_andNothingOnASecondCheck() {
        AccountId client = insertClientWithPlan();

        detection.check(client);
        detection.check(client);

        PlanChange stored = new TransactionTemplate(transactions)
                .execute(status -> planChanges.findOpen(client))
                .orElseThrow();
        assertThat(stored.getRule()).isEqualTo(Rule.LOW_COMPLETION);
        assertThat(stored.getState()).isEqualTo(PlanChangeState.PENDING);
        assertThat(stored.getPlanVersion()).isEqualTo(2);
        assertThat(stored.getEffectiveWeek()).isEqualTo(LocalDate.parse("2026-11-16"));
        // Wednesday and Friday were never finished: the later weekday goes
        assertThat(stored.getContent().removed()).contains(new RemovedSession(DayOfWeek.FRIDAY, "Session FRIDAY"));
        assertThat(stored.getFigures())
                .filteredOn(figure -> figure.kind() == FigureKind.SESSIONS_FINISHED)
                .extracting(figure -> figure.value().stripTrailingZeros())
                .containsExactly(BigDecimal.ONE, BigDecimal.ONE);
        assertThat(jdbc.queryForObject(
                        "select count(*) from plan_changes where client_id = ?", Integer.class, client.value()))
                .isEqualTo(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Wiring {

        @Bean
        DetectPlanChangesUseCase detectPlanChangesUseCase(
                TrainingHistory history, PlanChangeRepository planChanges, PlanSource plans) {
            PlanChangeDraftingService drafting = new PlanChangeDraftingService(
                    messages -> {
                        throw new PlanChangeDraftingFailedException("always fails");
                    },
                    new PlanChangeLimits(List.of(new LowCompletionLimits(), new HighRpeLimits(), new StalledWeightLimits())),
                    List.of(new LowCompletionFallback(), new HighRpeFallback(), new StalledWeightFallback()));
            return new PlanChangeDetectionService(
                    history,
                    planChanges,
                    List.of(new LowCompletionRule(), new HighRpeRule(), new StalledWeightRule()),
                    plans,
                    drafting,
                    new TrainerOfEveryClient(AccountId.random()),
                    notice -> {},
                    Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }
}
