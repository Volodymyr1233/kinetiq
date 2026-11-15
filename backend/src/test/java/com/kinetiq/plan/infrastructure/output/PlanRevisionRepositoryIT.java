package com.kinetiq.plan.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanRevision;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// accounts and exercises are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, PlanRepositoryImpl.class, PlanMapperImpl.class})
class PlanRevisionRepositoryIT {

    private static final LocalDate FIRST_WEEK = LocalDate.parse("2026-09-28");
    private static final LocalDate MONDAY = LocalDate.parse("2026-11-16");

    @Autowired
    private PlanRepositoryImpl repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID clientId;
    private UUID trainerId;

    private AccountId insertClient() {
        trainerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Marek Nowak', ?, 'hash', 'TRAINER', null, 'Europe/Warsaw', true, null, now(), now(), 0),
                       (?, 'Ola Kowalska', ?, 'hash', 'CLIENT', ?, 'Europe/Warsaw', true, now(), now(), now(), 0)
                """,
                trainerId,
                trainerId + "@example.com",
                clientId,
                clientId + "@example.com",
                trainerId);
        return new AccountId(clientId);
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

    private Plan publishedPlan(AccountId client, List<Session> sessions) {
        Plan stored = repository.save(Plan.draft(client, Goal.BUILD_MUSCLE, 12, sessions));
        Plan published = repository.save(stored.publish(FIRST_WEEK));
        entityManager.flush();
        return published;
    }

    private static List<Session> sessions(ExerciseId first, ExerciseId second) {
        return List.of(
                new Session("Push", DayOfWeek.MONDAY, List.of(
                        new ExerciseEntry(first, 3, 8, new BigDecimal("60.5"), 8, 90),
                        new ExerciseEntry(second, 4, 10, null, 7, 60))),
                new Session("Pull", DayOfWeek.THURSDAY, List.of(new ExerciseEntry(second, 3, 12, null, 8, 90))));
    }

    @Test
    void shouldReadBackASavedRevisionWithItsSessionsAndEntriesInOrder_whenRead() {
        AccountId client = insertClient();
        ExerciseId first = insertExercise();
        ExerciseId second = insertExercise();
        Plan plan = publishedPlan(client, sessions(first, second));

        repository.saveRevision(new PlanRevision(plan.getId(), MONDAY, sessions(first, second)));
        entityManager.clear();

        assertThat(repository.revisionsOf(plan.getId()))
                .singleElement()
                .satisfies(revision -> {
                    assertThat(revision.validUntil()).isEqualTo(MONDAY);
                    assertThat(revision.sessions()).isEqualTo(sessions(first, second));
                });
    }

    @Test
    void shouldListRevisionsOldestFirst_whenThereAreSeveral() {
        AccountId client = insertClient();
        ExerciseId exercise = insertExercise();
        Plan plan = publishedPlan(client, sessions(exercise, exercise));

        repository.saveRevision(new PlanRevision(plan.getId(), MONDAY.plusWeeks(2), sessions(exercise, exercise)));
        repository.saveRevision(new PlanRevision(plan.getId(), MONDAY, sessions(exercise, exercise)));
        entityManager.clear();

        assertThat(repository.revisionsOf(plan.getId()))
                .extracting(PlanRevision::validUntil)
                .containsExactly(MONDAY, MONDAY.plusWeeks(2));
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenASecondRevisionEndsOnTheSameMonday() {
        AccountId client = insertClient();
        ExerciseId exercise = insertExercise();
        Plan plan = publishedPlan(client, sessions(exercise, exercise));
        repository.saveRevision(new PlanRevision(plan.getId(), MONDAY, sessions(exercise, exercise)));

        assertThatThrownBy(() ->
                        repository.saveRevision(new PlanRevision(plan.getId(), MONDAY, sessions(exercise, exercise))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenARevisionEndsOnADayThatIsNotAMonday() {
        AccountId client = insertClient();
        ExerciseId exercise = insertExercise();
        Plan plan = publishedPlan(client, sessions(exercise, exercise));
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> jdbc.update(
                        "insert into plan_revisions (id, plan_id, valid_until, created_at) values (?, ?, ?, now())",
                        id,
                        plan.getId().value(),
                        MONDAY.plusDays(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRemoveTheRevisionRows_whenTheAccountIsDeleted() {
        AccountId client = insertClient();
        ExerciseId exercise = insertExercise();
        Plan plan = publishedPlan(client, sessions(exercise, exercise));
        repository.saveRevision(new PlanRevision(plan.getId(), MONDAY, sessions(exercise, exercise)));
        entityManager.flush();

        jdbc.update("delete from accounts where id = ?", clientId);

        assertThat(jdbc.queryForObject("select count(*) from plan_revisions", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from plan_revision_sessions", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from plan_revision_session_entries", Integer.class)).isZero();
    }
}
