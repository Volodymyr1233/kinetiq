package com.kinetiq.overview.infrastructure.output;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.overview.domain.model.ClientTraining;
import com.kinetiq.overview.domain.model.ClientTraining.DayCheckIn;
import com.kinetiq.overview.domain.model.ClientTraining.DayLog;
import com.kinetiq.overview.domain.model.ClientTraining.LastLog;
import com.kinetiq.overview.domain.model.PlanDates;
import com.kinetiq.overview.domain.model.RpeTotal;
import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.TargetChoice;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts, plans, logs and check-ins are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcTrainingRecords.class})
class JdbcTrainingRecordsIT {

    private static final LocalDate FIRST_MONDAY = LocalDate.parse("2026-10-05");
    private static final LocalDate SINCE = LocalDate.parse("2026-10-15");

    @Autowired
    private JdbcTrainingRecords records;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID trainer;

    @BeforeEach
    void insertTrainer() {
        trainer = insertAccount("TRAINER", null);
    }

    private UUID insertAccount(String role, UUID trainerId) {
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
                trainerId,
                role);
        return id;
    }

    // published on the Wednesday of its first week, or a draft when firstWeek is null
    private void insertPlan(UUID client, LocalDate firstWeek, String... days) {
        UUID plan = UUID.randomUUID();
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at, version)
                values (?, ?, 'BUILD_MUSCLE', 8, ?, ?, now(), now(), 0)
                """,
                plan,
                client,
                firstWeek,
                firstWeek == null ? null : firstWeek.plusDays(2));
        for (String day : days) {
            jdbc.update(
                    "insert into plan_sessions (id, plan_id, name, day_of_week) values (?, ?, 'Session', ?)",
                    UUID.randomUUID(),
                    plan,
                    day);
        }
    }

    private UUID insertLog(UUID client, String date, String name, boolean finished) {
        UUID log = UUID.randomUUID();
        jdbc.update(
                """
                insert into session_logs (id, client_id, log_date, session_name, started_at, finished_at, created_at,
                                          updated_at, version)
                values (?, ?, ?, ?, now(), case when ? then now() end, now(), now(), 0)
                """,
                log,
                client,
                LocalDate.parse(date),
                name,
                finished);
        UUID exercise = UUID.randomUUID();
        jdbc.update(
                """
                insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount,
                                       rest_seconds, cues, created_at, updated_at, version)
                values (?, null, ?, 'LEGS', '', 'REPS', 3, 10, 90, '', now(), now(), 0)
                """,
                exercise,
                "Exercise " + exercise);
        jdbc.update(
                """
                insert into session_log_entries (session_log_id, position, exercise_id, sets, amount, load_kg, rpe,
                                                 rest_seconds)
                values (?, 0, ?, 3, 8, null, 8, 90)
                """,
                log,
                exercise);
        return log;
    }

    private void insertSet(UUID log, int setNumber, int rpe, String loggedAt) {
        jdbc.update(
                """
                insert into set_logs (id, session_log_id, entry_position, set_number, load_kg, amount, rpe,
                                      target_load_kg, target_amount, target_rpe, logged_at, created_at, updated_at,
                                      version)
                values (?, ?, 0, ?, null, 8, ?, null, 8, 8, ?::timestamptz, now(), now(), 0)
                """,
                UUID.randomUUID(),
                log,
                setNumber,
                rpe,
                loggedAt);
    }

    private void insertCheckIn(UUID client, String date, int sleep, String choice) {
        jdbc.update(
                """
                insert into check_ins (id, client_id, check_in_date, sleep, soreness, stress, saved_at, created_at,
                                       updated_at, version, target_choice)
                values (?, ?, ?, ?, 4, 4, now(), now(), now(), 0, ?)
                """,
                UUID.randomUUID(),
                client,
                LocalDate.parse(date),
                sleep,
                choice);
    }

    @Test
    void shouldReadThePlanLogsLastSetAndCheckIns_ofEachClient() {
        UUID client = insertAccount("CLIENT", trainer);
        insertPlan(client, FIRST_MONDAY, "MONDAY", "FRIDAY");
        UUID monday = insertLog(client, "2026-11-09", "Lower body", true);
        insertSet(monday, 1, 7, "2026-11-09T17:00:00Z");
        insertSet(monday, 2, 9, "2026-11-09T17:05:00Z");
        UUID friday = insertLog(client, "2026-11-13", "Upper body", false);
        insertSet(friday, 1, 8, "2026-11-13T08:00:00Z");
        insertLog(client, "2026-11-16", "Started only", false);
        insertCheckIn(client, "2026-11-13", 1, "ORIGINAL");

        ClientTraining training = records.forTrainer(new AccountId(trainer), SINCE).get(new AccountId(client));

        assertThat(training.plan())
                .isEqualTo(new PlanDates(FIRST_MONDAY, FIRST_MONDAY.plusDays(2), 8, Set.of(MONDAY, FRIDAY)));
        assertThat(training.days())
                .isEqualTo(Map.of(
                        LocalDate.parse("2026-11-09"), new DayLog(true, new RpeTotal(16, 2)),
                        LocalDate.parse("2026-11-13"), new DayLog(false, new RpeTotal(8, 1)),
                        LocalDate.parse("2026-11-16"), new DayLog(false, RpeTotal.NONE)));
        assertThat(training.lastLog())
                .isEqualTo(new LastLog(
                        Instant.parse("2026-11-13T08:00:00Z"), LocalDate.parse("2026-11-13"), "Upper body"));
        assertThat(training.checkIns())
                .isEqualTo(Map.of(
                        LocalDate.parse("2026-11-13"),
                        new DayCheckIn(new Answers(1, 4, 4), TargetChoice.ORIGINAL)));
    }

    @Test
    void shouldLeaveOutLogsAndCheckInsBeforeSince_butKeepTheLastSetEver() {
        UUID client = insertAccount("CLIENT", trainer);
        insertPlan(client, FIRST_MONDAY, "MONDAY");
        UUID old = insertLog(client, "2026-10-12", "Old", true);
        insertSet(old, 1, 8, "2026-10-12T17:00:00Z");
        insertCheckIn(client, "2026-10-12", 3, "LIGHTER");

        ClientTraining training = records.forTrainer(new AccountId(trainer), SINCE).get(new AccountId(client));

        assertThat(training.days()).isEmpty();
        assertThat(training.checkIns()).isEmpty();
        assertThat(training.lastLog().sessionName()).isEqualTo("Old");
    }

    @Test
    void shouldHaveNoPlanDates_forADraft() {
        UUID client = insertAccount("CLIENT", trainer);
        insertPlan(client, null, "MONDAY");
        insertCheckIn(client, "2026-11-13", 3, "LIGHTER");

        assertThat(records.forTrainer(new AccountId(trainer), SINCE).get(new AccountId(client)).plan())
                .isNull();
    }

    @Test
    void shouldNeverReadAnotherTrainersClients() {
        UUID other = insertAccount("TRAINER", null);
        UUID theirs = insertAccount("CLIENT", other);
        insertPlan(theirs, FIRST_MONDAY, "MONDAY");
        insertSet(insertLog(theirs, "2026-11-09", "Theirs", true), 1, 8, "2026-11-09T17:00:00Z");
        insertCheckIn(theirs, "2026-11-13", 1, "LIGHTER");
        UUID mine = insertAccount("CLIENT", trainer);

        assertThat(records.forTrainer(new AccountId(trainer), SINCE)).doesNotContainKey(new AccountId(theirs));
        assertThat(records.forTrainer(new AccountId(other), SINCE)).doesNotContainKey(new AccountId(mine));
    }
}
