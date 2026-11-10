package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.planchange.domain.model.EntrySnapshot;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.SessionSnapshot;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts, exercises and plans are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcPlanSource.class})
class JdbcPlanSourceIT {

    private static final LocalDate FIRST_WEEK = LocalDate.parse("2026-10-05");

    @Autowired
    private JdbcPlanSource source;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId insertClient() {
        UUID trainer = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        for (UUID id : new UUID[] {trainer, client}) {
            jdbc.update(
                    """
                    insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                          health_data_consent_at, created_at, updated_at, version)
                    values (?, 'Account', ?, 'hash', ?, ?, 'Europe/Warsaw', true,
                            case when ? = 'CLIENT' then now() end, now(), now(), 0)
                    """,
                    id,
                    id + "@example.com",
                    id == trainer ? "TRAINER" : "CLIENT",
                    id == trainer ? null : trainer,
                    id == trainer ? "TRAINER" : "CLIENT");
        }
        return new AccountId(client);
    }

    private ExerciseId insertExercise(String category, String measure, String name, String polishName) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into exercises (id, owner_id, name, name_pl, category, equipment, measure, default_sets,
                                       default_amount, rest_seconds, cues, created_at, updated_at, version)
                values (?, null, ?, ?, ?, '', ?, 3, 10, 90, '', now(), now(), 0)
                """,
                id,
                name + " " + id,
                polishName,
                category,
                measure);
        return new ExerciseId(id);
    }

    private UUID insertPlan(AccountId client, LocalDate firstWeek, long version) {
        UUID plan = UUID.randomUUID();
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at, version)
                values (?, ?, 'LOSE_FAT', 8, ?, ?, now(), now(), ?)
                """,
                plan,
                client.value(),
                firstWeek,
                firstWeek,
                version);
        return plan;
    }

    private UUID insertSession(UUID plan, String day, String name) {
        UUID session = UUID.randomUUID();
        jdbc.update(
                "insert into plan_sessions (id, plan_id, name, day_of_week) values (?, ?, ?, ?)", session, plan, name, day);
        return session;
    }

    private void insertEntry(UUID session, int position, ExerciseId exercise, int amount, String load) {
        jdbc.update(
                """
                insert into plan_session_entries (session_id, position, exercise_id, sets, amount, load_kg, rpe,
                                                  rest_seconds)
                values (?, ?, ?, 3, ?, ?::numeric, 8, 120)
                """,
                session,
                position,
                exercise.value(),
                amount,
                load);
    }

    @Test
    void shouldReadThePublishedPlan_withSessionsInWeekOrderAndEntriesInPlace() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise("LEGS", "REPS", "Squat", null);
        ExerciseId plank = insertExercise("CORE", "SECONDS", "Plank", null);
        UUID plan = insertPlan(client, FIRST_WEEK, 5);
        UUID friday = insertSession(plan, "FRIDAY", "Full body");
        UUID monday = insertSession(plan, "MONDAY", "Lower");
        insertEntry(monday, 1, plank, 45, null);
        insertEntry(monday, 0, squat, 8, "100.0");
        insertEntry(friday, 0, squat, 5, "110.0");

        PlanSnapshot snapshot = source.currentPlan(client).orElseThrow();

        assertThat(snapshot.version()).isEqualTo(5);
        assertThat(snapshot.firstWeek()).isEqualTo(FIRST_WEEK);
        assertThat(snapshot.weeks()).isEqualTo(8);
        assertThat(snapshot.sessions()).extracting(SessionSnapshot::day).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        assertThat(snapshot.sessions().getFirst().name()).isEqualTo("Lower");
        assertThat(snapshot.sessions().getFirst().entries())
                .containsExactly(
                        new EntrySnapshot(squat, Category.LEGS, Measure.REPS, 3, 8, new BigDecimal("100.0"), 8, 120),
                        new EntrySnapshot(plank, Category.CORE, Measure.SECONDS, 3, 45, null, 8, 120));
    }

    @Test
    void shouldFindNoPlan_forADraftOrAnotherClient() {
        AccountId withDraft = insertClient();
        jdbc.update(
                "insert into plans (id, client_id, goal, weeks, created_at, updated_at, version)"
                        + " values (?, ?, 'LOSE_FAT', 8, now(), now(), 0)",
                UUID.randomUUID(),
                withDraft.value());
        AccountId other = insertClient();
        insertPlan(other, FIRST_WEEK, 0);

        assertThat(source.currentPlan(withDraft)).isEmpty();
        assertThat(source.currentPlan(insertClient())).isEmpty();
    }

    @Test
    void shouldNameExercisesInTheLanguage_withEnglishWhenThereIsNoPolishName() {
        ExerciseId squat = insertExercise("LEGS", "REPS", "Squat", "Przysiad");
        ExerciseId own = insertExercise("LEGS", "REPS", "Sled push", null);

        ExerciseId plank = insertExercise("CORE", "SECONDS", "Plank", "Deska");

        assertThat(source.exercises(Set.of(squat, own, plank), Language.PL))
                .containsEntry(squat, new ExerciseLabel("Przysiad", Measure.REPS))
                .containsEntry(own, new ExerciseLabel("Sled push " + own.value(), Measure.REPS))
                .containsEntry(plank, new ExerciseLabel("Deska", Measure.SECONDS));
        assertThat(source.exercises(Set.of(squat), Language.EN))
                .containsEntry(squat, new ExerciseLabel("Squat " + squat.value(), Measure.REPS));
        assertThat(source.exercises(Set.of(), Language.EN)).isEmpty();
    }
}
