package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.LoggedSet;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
    private ExerciseId insertExercise() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount,
                                       rest_seconds, cues, created_at, updated_at, version)
                values (?, null, ?, 'LEGS', '', 'REPS', 3, 10, 90, '', now(), now(), 0)
                """,
                id,
                "Exercise " + id);
        return new ExerciseId(id);
    }

    // a plan of Monday, Wednesday and Friday, published on the Wednesday of its first week
    private void insertThreeDayPlan(AccountId client) {
        UUID plan = UUID.randomUUID();
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at, version)
                values (?, ?, 'LOSE_FAT', 8, ?, ?, now(), now(), 0)
                """,
                plan,
                client.value(),
                FIRST_WEEK,
                FIRST_WEEK.plusDays(2));
        for (String day : List.of("MONDAY", "WEDNESDAY", "FRIDAY")) {
            jdbc.update(
                    "insert into plan_sessions (id, plan_id, name, day_of_week) values (?, ?, 'Session', ?)",
                    UUID.randomUUID(),
                    plan,
                    day);
        }
    }

    // a session log with one entry for the exercise and one set per RPE given
    private void insertLog(AccountId client, String date, boolean finished, ExerciseId exercise, int... rpes) {
        UUID log = UUID.randomUUID();
        jdbc.update(
                """
                insert into session_logs (id, client_id, log_date, session_name, started_at, finished_at, created_at,
                                          updated_at, version)
                values (?, ?, ?, 'Session', now(), case when ? then now() end, now(), now(), 0)
                """,
                log,
                client.value(),
                LocalDate.parse(date),
                finished);
        jdbc.update(
                """
                insert into session_log_entries (session_log_id, position, exercise_id, sets, amount, load_kg, rpe,
                                                 rest_seconds)
                values (?, 0, ?, 3, 8, 100.0, 8, 90)
                """,
                log,
                exercise.value());
        for (int set = 0; set < rpes.length; set++) {
            jdbc.update(
                    """
                    insert into set_logs (id, session_log_id, entry_position, set_number, load_kg, amount, rpe,
                                          target_load_kg, target_amount, target_rpe, logged_at, created_at, updated_at,
                                          version)
                    values (?, ?, 0, ?, 102.5, 8, ?, 100.0, 8, 8, now(), now(), now(), 0)
                    """,
                    UUID.randomUUID(),
                    log,
                    set + 1,
                    rpes[set]);
        }
    }

    @Test
    void shouldCountPlannedAndFinishedSessionsPerWeek_fromThePublishingDay() {
        AccountId client = insertClient("Europe/Warsaw");
        insertThreeDayPlan(client);
        ExerciseId squat = insertExercise();
        insertLog(client, "2026-10-07", true, squat, 8);
        insertLog(client, "2026-10-09", false, squat, 9);
        insertLog(client, "2026-10-12", true, squat, 8);
        insertLog(client, "2026-10-14", true, squat, 8);

        List<WeekTraining> weeks = history.weeks(client, FIRST_WEEK, 2);

        assertThat(weeks).extracting(WeekTraining::monday).containsExactly(FIRST_WEEK, FIRST_WEEK.plusWeeks(1));
        assertThat(weeks).extracting(WeekTraining::sessionsInPlan).containsExactly(3, 3);
        // the Monday before the publishing day is not planned, and a started but unfinished session is not finished
        assertThat(weeks).extracting(WeekTraining::plannedSessions).containsExactly(2, 3);
        assertThat(weeks).extracting(WeekTraining::finishedSessions).containsExactly(1, 2);
    }

    @Test
    void shouldReturnEverySetWithItsExerciseLoadAndRpe_inTheWeekOfItsLog() {
        AccountId client = insertClient("Europe/Warsaw");
        insertThreeDayPlan(client);
        ExerciseId squat = insertExercise();
        ExerciseId row = insertExercise();
        insertLog(client, "2026-10-07", true, squat, 8, 9);
        insertLog(client, "2026-10-14", true, row, 7);

        List<WeekTraining> weeks = history.weeks(client, FIRST_WEEK, 2);

        assertThat(weeks.get(0).sets())
                .containsExactlyInAnyOrder(
                        new LoggedSet(squat, new BigDecimal("102.5"), 8), new LoggedSet(squat, new BigDecimal("102.5"), 9));
        assertThat(weeks.get(1).sets()).containsExactly(new LoggedSet(row, new BigDecimal("102.5"), 7));
    }

    @Test
    void shouldLeaveOutDaysOutsideTheRequestedWeeks() {
        AccountId client = insertClient("Europe/Warsaw");
        insertThreeDayPlan(client);
        ExerciseId squat = insertExercise();
        insertLog(client, "2026-10-07", true, squat, 8);
        insertLog(client, "2026-10-14", true, squat, 8);

        List<WeekTraining> weeks = history.weeks(client, FIRST_WEEK.plusWeeks(1), 1);

        assertThat(weeks.getFirst().finishedSessions()).isEqualTo(1);
        assertThat(weeks.getFirst().sets()).hasSize(1);
    }

    @Test
    void shouldNeverShowAnotherClientsLogs() {
        AccountId client = insertClient("Europe/Warsaw");
        AccountId other = insertClient("Europe/Warsaw");
        insertThreeDayPlan(client);
        insertThreeDayPlan(other);
        ExerciseId squat = insertExercise();
        insertLog(other, "2026-10-07", true, squat, 10, 10);

        WeekTraining week = history.weeks(client, FIRST_WEEK, 1).getFirst();

        assertThat(week.finishedSessions()).isZero();
        assertThat(week.sets()).isEmpty();
    }
    @Test
    void shouldLeaveOutAClientWithAnInvalidTimeZone_andListTheOthers() {
        AccountId broken = insertClient("Not/AZone");
        insertPlan(broken, FIRST_WEEK);
        AccountId fine = insertClient("Europe/Warsaw");
        insertPlan(fine, FIRST_WEEK);

        assertThat(listed(broken)).isEmpty();
        assertThat(history.scheduledClient(broken)).isEmpty();
        assertThat(listed(fine)).isPresent();
    }
}
