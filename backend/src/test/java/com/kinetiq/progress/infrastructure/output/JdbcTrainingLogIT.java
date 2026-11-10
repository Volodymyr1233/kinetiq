package com.kinetiq.progress.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.progress.domain.model.LoggedExercise;
import com.kinetiq.progress.domain.model.TopSet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts, exercises and logs are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcTrainingLog.class})
class JdbcTrainingLogIT {

    private static final String MONDAY = "2026-10-05";
    private static final String WEDNESDAY = "2026-10-07";

    @Autowired
    private JdbcTrainingLog trainingLog;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId insertClient() {
        UUID trainer = UUID.randomUUID();
        insertAccount(trainer, "TRAINER", null);
        UUID id = UUID.randomUUID();
        insertAccount(id, "CLIENT", trainer);
        return new AccountId(id);
    }

    private void insertAccount(UUID id, String role, UUID trainer) {
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
    }

    private ExerciseId insertExercise(String measure) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount,
                                       rest_seconds, cues, created_at, updated_at, version)
                values (?, null, ?, 'LEGS', '', ?, 3, 10, 90, '', now(), now(), 0)
                """,
                id,
                "Exercise " + id,
                measure);
        return new ExerciseId(id);
    }

    private UUID insertLog(AccountId client, String date, boolean finished) {
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
        return log;
    }

    private void insertEntry(UUID log, int position, ExerciseId exercise) {
        jdbc.update(
                """
                insert into session_log_entries (session_log_id, position, exercise_id, sets, amount, load_kg, rpe,
                                                 rest_seconds)
                values (?, ?, ?, 3, 8, null, 8, 90)
                """,
                log,
                position,
                exercise.value());
    }

    // loadKg is null for a set without a load
    private void insertSet(UUID log, int position, int setNumber, String loadKg, int amount, int rpe) {
        jdbc.update(
                """
                insert into set_logs (id, session_log_id, entry_position, set_number, load_kg, amount, rpe,
                                      target_load_kg, target_amount, target_rpe, logged_at, created_at, updated_at,
                                      version)
                values (?, ?, ?, ?, ?, ?, ?, null, 8, 8, now(), now(), now(), 0)
                """,
                UUID.randomUUID(),
                log,
                position,
                setNumber,
                loadKg == null ? null : new BigDecimal(loadKg),
                amount,
                rpe);
    }

    private static TopSet set(String loadKg, int amount, int rpe) {
        return new TopSet(loadKg == null ? null : new BigDecimal(loadKg), amount, rpe);
    }

    @Nested
    class LoggedExercises {

        @Test
        void shouldTakeTheSetWithALoad_whenAnotherHasMoreRepetitionsAndNoLoad() {
            AccountId client = insertClient();
            ExerciseId squat = insertExercise("REPS");
            UUID log = insertLog(client, MONDAY, true);
            insertEntry(log, 0, squat);
            insertSet(log, 0, 1, null, 20, 7);
            insertSet(log, 0, 2, "20.0", 5, 8);

            assertThat(trainingLog.loggedExercises(client))
                    .extracting(LoggedExercise::top)
                    .containsExactly(set("20.0", 5, 8));
        }

        @Test
        void shouldTakeTheHigherLoad() {
            AccountId client = insertClient();
            ExerciseId squat = insertExercise("REPS");
            UUID log = insertLog(client, MONDAY, true);
            insertEntry(log, 0, squat);
            insertSet(log, 0, 1, "100.0", 8, 7);
            insertSet(log, 0, 2, "102.5", 5, 9);
            insertSet(log, 0, 3, "97.5", 10, 8);

            assertThat(trainingLog.loggedExercises(client))
                    .extracting(LoggedExercise::top)
                    .containsExactly(set("102.5", 5, 9));
        }

        @Test
        void shouldTakeMoreRepetitions_whenLoadsAreEqual() {
            AccountId client = insertClient();
            ExerciseId squat = insertExercise("REPS");
            UUID log = insertLog(client, MONDAY, true);
            insertEntry(log, 0, squat);
            insertSet(log, 0, 1, "100.0", 6, 8);
            insertSet(log, 0, 2, "100.0", 8, 9);

            assertThat(trainingLog.loggedExercises(client))
                    .extracting(LoggedExercise::top)
                    .containsExactly(set("100.0", 8, 9));
        }

        @Test
        void shouldTakeTheEarlierSetInTheSession_whenSetsAreEqual() {
            AccountId client = insertClient();
            ExerciseId squat = insertExercise("REPS");
            UUID log = insertLog(client, MONDAY, true);
            insertEntry(log, 0, squat);
            insertEntry(log, 1, squat);
            // stored in reverse, so the order of insertion cannot pick the winner
            insertSet(log, 1, 1, "100.0", 8, 9);
            insertSet(log, 0, 2, "100.0", 8, 8);
            insertSet(log, 0, 1, "100.0", 8, 7);

            assertThat(trainingLog.loggedExercises(client))
                    .extracting(LoggedExercise::top)
                    .containsExactly(set("100.0", 8, 7));
        }

        @Test
        void shouldGiveOneRowWithBothSums_whenAnExerciseIsInTwoEntriesOfASession() {
            AccountId client = insertClient();
            ExerciseId squat = insertExercise("REPS");
            ExerciseId row = insertExercise("REPS");
            UUID log = insertLog(client, MONDAY, false);
            insertEntry(log, 0, squat);
            insertEntry(log, 1, row);
            insertEntry(log, 2, squat);
            insertSet(log, 0, 1, "100.0", 8, 7);
            insertSet(log, 1, 1, "60.0", 10, 8);
            insertSet(log, 2, 1, "90.0", 10, 9);
            insertSet(log, 2, 2, "90.0", 10, 10);

            assertThat(trainingLog.loggedExercises(client))
                    .containsExactlyInAnyOrder(
                            new LoggedExercise(LocalDate.parse(MONDAY), squat, set("100.0", 8, 7), 26, 3),
                            new LoggedExercise(LocalDate.parse(MONDAY), row, set("60.0", 10, 8), 8, 1));
        }

        @Test
        void shouldTakeSecondsAsTheAmount_forAnExerciseInSeconds() {
            AccountId client = insertClient();
            ExerciseId plank = insertExercise("SECONDS");
            UUID log = insertLog(client, MONDAY, true);
            insertEntry(log, 0, plank);
            insertSet(log, 0, 1, null, 45, 7);
            insertSet(log, 0, 2, null, 60, 8);

            assertThat(trainingLog.loggedExercises(client))
                    .extracting(LoggedExercise::top)
                    .containsExactly(set(null, 60, 8));
        }

        @Test
        void shouldListOneRowPerSessionLog_oldestFirst() {
            AccountId client = insertClient();
            ExerciseId squat = insertExercise("REPS");
            UUID later = insertLog(client, WEDNESDAY, true);
            insertEntry(later, 0, squat);
            insertSet(later, 0, 1, "105.0", 5, 8);
            UUID earlier = insertLog(client, MONDAY, true);
            insertEntry(earlier, 0, squat);
            insertSet(earlier, 0, 1, "100.0", 5, 8);

            assertThat(trainingLog.loggedExercises(client))
                    .extracting(LoggedExercise::date)
                    .containsExactly(LocalDate.parse(MONDAY), LocalDate.parse(WEDNESDAY));
        }

        @Test
        void shouldGiveNoRow_whenASessionHasNoLoggedSet() {
            AccountId client = insertClient();
            UUID log = insertLog(client, MONDAY, false);
            insertEntry(log, 0, insertExercise("REPS"));

            assertThat(trainingLog.loggedExercises(client)).isEmpty();
        }

        @Test
        void shouldNeverReadAnotherClientsLogs() {
            AccountId client = insertClient();
            AccountId other = insertClient();
            UUID log = insertLog(other, MONDAY, true);
            insertEntry(log, 0, insertExercise("REPS"));
            insertSet(log, 0, 1, "100.0", 8, 8);

            assertThat(trainingLog.loggedExercises(client)).isEmpty();
        }
    }

    @Nested
    class FinishedDays {

        @Test
        void shouldHoldTheClientsFinishedSessionLogsOnly() {
            AccountId client = insertClient();
            AccountId other = insertClient();
            insertLog(client, MONDAY, true);
            insertLog(client, WEDNESDAY, false);
            insertLog(other, "2026-10-09", true);

            assertThat(trainingLog.finishedDays(client)).containsExactly(LocalDate.parse(MONDAY));
        }
    }
}
