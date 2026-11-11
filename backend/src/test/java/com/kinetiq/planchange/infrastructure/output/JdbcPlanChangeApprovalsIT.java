package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.output.PlanChangeApprovals;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts and plan changes are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcPlanChangeApprovals.class})
class JdbcPlanChangeApprovalsIT {

    private static final Instant SINCE = Instant.parse("2026-10-13T10:00:00Z");

    @Autowired
    private JdbcPlanChangeApprovals approvals;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID insertAccount(String name, String role, UUID trainer) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, ?, ?, 'hash', ?, ?, 'Europe/Warsaw', true,
                        case when ? = 'CLIENT' then now() end, now(), now(), 0)
                """,
                id,
                name,
                id + "@example.com",
                role,
                trainer,
                role);
        return id;
    }

    private PlanChangeApprovals.Entry open(UUID client, Instant createdAt) {
        return insert(client, "PENDING", createdAt, null);
    }

    private PlanChangeApprovals.Entry declined(UUID client, Instant createdAt, Instant endedAt) {
        return insert(client, "DECLINED", createdAt, endedAt);
    }

    private PlanChangeApprovals.Entry insert(UUID client, String state, Instant createdAt, Instant endedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into plan_changes (id, client_id, rule, state, plan_version, window_start, window_end,
                                          effective_week, created_at, ended_at, declined_by, decline_reason)
                values (?, ?, 'HIGH_RPE', ?, 0, date '2026-10-26', date '2026-11-08', date '2026-11-16', ?, ?,
                        case when ? = 'DECLINED' then 'CLIENT' end, case when ? = 'DECLINED' then 'OTHER' end)
                """,
                id,
                client,
                state,
                Timestamp.from(createdAt),
                endedAt == null ? null : Timestamp.from(endedAt),
                state,
                state);
        return new PlanChangeApprovals.Entry(new PlanChangeId(id), null);
    }

    private static List<UUID> ids(List<PlanChangeApprovals.Entry> entries) {
        return entries.stream().map(entry -> entry.id().value()).toList();
    }

    @Test
    void shouldListTheOpenOnesAndThoseEndedFromTheBoundaryOn_newestFirst_withTheClientsName() {
        UUID trainer = insertAccount("Trainer", "TRAINER", null);
        UUID ola = insertAccount("Ola Nowak", "CLIENT", trainer);
        UUID jan = insertAccount("Jan Kowal", "CLIENT", trainer);
        Instant created = Instant.parse("2026-09-01T10:00:00Z");
        PlanChangeApprovals.Entry endedDayAfter = declined(ola, created, SINCE.plusSeconds(86_400));
        PlanChangeApprovals.Entry endedOnTheBoundary = declined(ola, created, SINCE);
        declined(ola, created, SINCE.minusSeconds(1));
        PlanChangeApprovals.Entry waiting = open(jan, Instant.parse("2026-11-10T10:00:00Z"));

        List<PlanChangeApprovals.Entry> rows = approvals.forTrainer(new AccountId(trainer), SINCE);

        assertThat(ids(rows)).containsExactly(waiting.id().value(), endedDayAfter.id().value(), endedOnTheBoundary.id().value());
        assertThat(rows).extracting(PlanChangeApprovals.Entry::clientName).containsExactly("Jan Kowal", "Ola Nowak", "Ola Nowak");
    }

    @Test
    void shouldNeverListAnotherTrainersClientsPlanChanges() {
        UUID trainer = insertAccount("Trainer", "TRAINER", null);
        UUID other = insertAccount("Other trainer", "TRAINER", null);
        UUID foreignClient = insertAccount("Foreign Client", "CLIENT", other);
        open(foreignClient, Instant.parse("2026-11-10T10:00:00Z"));
        declined(foreignClient, Instant.parse("2026-09-01T10:00:00Z"), Instant.parse("2026-11-01T10:00:00Z"));

        assertThat(approvals.forTrainer(new AccountId(trainer), SINCE)).isEmpty();
        assertThat(approvals.forTrainer(new AccountId(other), SINCE)).hasSize(2);
    }
}
