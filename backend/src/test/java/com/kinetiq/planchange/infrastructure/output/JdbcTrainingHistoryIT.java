package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts and plans are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcTrainingHistory.class})
class JdbcTrainingHistoryIT {

    private static final LocalDate FIRST_WEEK = LocalDate.parse("2026-10-05");
    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    @Autowired
    private JdbcTrainingHistory history;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId insertClient(String zone) {
        UUID trainer = UUID.randomUUID();
        insertAccount(trainer, "TRAINER", null, zone);
        UUID id = UUID.randomUUID();
        insertAccount(id, "CLIENT", trainer, zone);
        return new AccountId(id);
    }

    private void insertAccount(UUID id, String role, UUID trainer, String zone) {
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Account', ?, 'hash', ?, ?, ?, true,
                        case when ? = 'CLIENT' then now() end, now(), now(), 0)
                """,
                id,
                id + "@example.com",
                role,
                trainer,
                zone,
                role);
    }

    private void insertPlan(AccountId client, LocalDate firstWeek) {
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at, version)
                values (?, ?, 'LOSE_FAT', 8, ?, ?, now(), now(), 0)
                """,
                UUID.randomUUID(),
                client.value(),
                firstWeek,
                firstWeek);
    }

    private Optional<ScheduledClient> listed(AccountId client) {
        return history.clientsWithPublishedPlan().stream()
                .filter(scheduled -> scheduled.client().equals(client))
                .findFirst();
    }

    @Test
    void shouldListAPublishedPlanWithItsZoneAndLength_andNoLastCheckedWeek() {
        AccountId client = insertClient("Europe/Warsaw");
        insertPlan(client, FIRST_WEEK);

        ScheduledClient scheduled = listed(client).orElseThrow();

        assertThat(scheduled.zone()).isEqualTo(ZoneId.of("Europe/Warsaw"));
        assertThat(scheduled.firstWeek()).isEqualTo(FIRST_WEEK);
        assertThat(scheduled.weeks()).isEqualTo(8);
        assertThat(scheduled.lastCheckedWeek()).isEmpty();
        assertThat(history.scheduledClient(client)).contains(scheduled);
    }

    @Test
    void shouldNotListADraft_andFindNoScheduleForIt() {
        AccountId draft = insertClient("Europe/Warsaw");
        jdbc.update(
                "insert into plans (id, client_id, goal, weeks, created_at, updated_at, version)"
                        + " values (?, ?, 'LOSE_FAT', 8, now(), now(), 0)",
                UUID.randomUUID(),
                draft.value());
        AccountId withoutPlan = insertClient("Europe/Warsaw");

        assertThat(listed(draft)).isEmpty();
        assertThat(history.scheduledClient(draft)).isEmpty();
        assertThat(history.scheduledClient(withoutPlan)).isEmpty();
    }

    @Test
    void shouldReportTheLatestCheckedWeek_whenWeeksWereClaimed() {
        AccountId client = insertClient("America/Los_Angeles");
        insertPlan(client, FIRST_WEEK);
        history.claimWeek(client, LocalDate.parse("2026-11-02"), NOW);
        history.claimWeek(client, LocalDate.parse("2026-10-26"), NOW);

        assertThat(listed(client).orElseThrow().lastCheckedWeek()).contains(LocalDate.parse("2026-11-02"));
        assertThat(history.scheduledClient(client).orElseThrow().lastCheckedWeek())
                .contains(LocalDate.parse("2026-11-02"));
    }

    @Test
    void shouldClaimAWeekOnce() {
        AccountId client = insertClient("Europe/Warsaw");
        LocalDate week = LocalDate.parse("2026-11-09");

        assertThat(history.claimWeek(client, week, NOW)).isTrue();
        assertThat(history.claimWeek(client, week, NOW)).isFalse();
        assertThat(history.claimWeek(client, week.plusDays(7), NOW)).isTrue();
    }

    @Test
    void shouldRejectAWeekThatDoesNotStartOnMonday() {
        AccountId client = insertClient("Europe/Warsaw");

        assertThatThrownBy(() -> history.claimWeek(client, LocalDate.parse("2026-11-10"), NOW))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
