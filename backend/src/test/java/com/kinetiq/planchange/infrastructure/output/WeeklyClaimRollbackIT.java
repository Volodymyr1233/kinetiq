package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.TransactionConfig;
import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import com.kinetiq.planchange.domain.rule.HighRpeFallback;
import com.kinetiq.planchange.domain.rule.HighRpeLimits;
import com.kinetiq.planchange.domain.rule.LowCompletionFallback;
import com.kinetiq.planchange.domain.rule.LowCompletionLimits;
import com.kinetiq.planchange.domain.rule.StalledWeightFallback;
import com.kinetiq.planchange.domain.rule.StalledWeightLimits;
import com.kinetiq.planchange.domain.service.PlanChangeDetectionService;
import com.kinetiq.planchange.domain.service.PlanChangeDraftingService;
import com.kinetiq.planchange.domain.service.PlanChangeLimits;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A check that fails after the week was claimed leaves no claim behind, so the next run tries again (FR-011). */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // the use case must open the transaction, not the test
@Import({
    TestcontainersConfiguration.class,
    JdbcTrainingHistory.class,
    PlanChangeRepositoryImpl.class,
    PlanChangeMapperImpl.class,
    TransactionConfig.class,
    WeeklyClaimRollbackIT.Wiring.class
})
class WeeklyClaimRollbackIT {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    @Autowired
    private DetectPlanChangesUseCase detection;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID client;
    private UUID trainer;

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from weekly_checks where client_id = ?", client);
        jdbc.update("delete from plans where client_id = ?", client);
        jdbc.update("delete from accounts where id = ?", client);
        jdbc.update("delete from accounts where id = ?", trainer);
    }

    @Test
    void shouldLeaveNoClaim_whenTheCheckFailsAfterClaimingTheWeek() {
        trainer = UUID.randomUUID();
        client = UUID.randomUUID();
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
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at, version)
                values (?, ?, 'LOSE_FAT', 8, date '2026-10-05', date '2026-10-05', now(), now(), 0)
                """,
                UUID.randomUUID(),
                client);

        assertThatThrownBy(() -> detection.check(new AccountId(client))).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("select count(*) from weekly_checks where client_id = ?", Integer.class, client))
                .isZero();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Wiring {

        @Bean
        DetectPlanChangesUseCase detectPlanChangesUseCase(
                TrainingHistory history, PlanChangeRepository planChanges, JdbcTemplate jdbc) {
            return new PlanChangeDetectionService(
                    new FailsAfterClaim(history),
                    planChanges,
                    List.of(),
                    new JdbcPlanSource(jdbc),
                    new PlanChangeDraftingService(
                            new UnavailablePlanChangeDrafter(),
                            new PlanChangeLimits(
                                    List.of(new LowCompletionLimits(), new HighRpeLimits(), new StalledWeightLimits())),
                            List.of(new LowCompletionFallback(), new HighRpeFallback(), new StalledWeightFallback())),
                    Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }

    /** Claims the week for real, then fails, as a later step of the check would. */
    private record FailsAfterClaim(TrainingHistory real) implements TrainingHistory {

        @Override
        public List<ScheduledClient> clientsWithPublishedPlan() {
            return real.clientsWithPublishedPlan();
        }

        @Override
        public Optional<ScheduledClient> scheduledClient(AccountId client) {
            return real.scheduledClient(client);
        }

        @Override
        public List<WeekTraining> weeks(AccountId client, LocalDate firstMonday, int count) {
            return real.weeks(client, firstMonday, count);
        }

        @Override
        public boolean claimWeek(AccountId client, LocalDate week, Instant now) {
            real.claimWeek(client, week, now);
            throw new IllegalStateException("fails after the claim");
        }
    }
}
