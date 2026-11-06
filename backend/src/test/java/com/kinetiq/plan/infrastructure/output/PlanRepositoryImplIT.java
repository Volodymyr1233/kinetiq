package com.kinetiq.plan.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.model.AutomaticDraft;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
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
class PlanRepositoryImplIT {

    @Autowired
    private PlanRepositoryImpl repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId insertClient() {
        UUID trainer = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Marek Nowak', ?, 'hash', 'TRAINER', null, 'Europe/Warsaw', true, null, now(), now(), 0),
                       (?, 'Ola Kowalska', ?, 'hash', 'CLIENT', ?, 'Europe/Warsaw', true, now(), now(), now(), 0)
                """,
                trainer,
                trainer + "@example.com",
                client,
                client + "@example.com",
                trainer);
        return new AccountId(client);
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

    private static ExerciseEntry entry(ExerciseId exercise, int sets) {
        return new ExerciseEntry(exercise, sets, 8, null, 8, 90);
    }

    private static Plan draft(AccountId client, List<Session> sessions) {
        return Plan.draft(client, Goal.BUILD_MUSCLE, 12, sessions);
    }

    private static Plan withSessions(Plan plan, List<Session> sessions) {
        return new Plan(
                plan.getId(),
                plan.getClient(),
                plan.getGoal(),
                plan.getWeeks(),
                plan.getFirstWeek().orElse(null),
                sessions,
                plan.getVersion());
    }

    // cleared, so what is read back comes from the database and not from Hibernate's cached entity
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldKeepTheMark_whenADraftIsReadBack() {
        AccountId failed = insertClient();
        repository.save(Plan.failed(failed, Goal.LOSE_FAT, 12));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByClient(failed).orElseThrow().getAutomaticDraft()).contains(AutomaticDraft.FAILED);
        assertThat(jdbc.queryForObject(
                        "select automatic_draft from plans where client_id = ?", String.class, failed.value()))
                .isEqualTo("FAILED");
    }

    @Test
    void shouldClearTheMarkInStorage_whenAnEditedPlanIsSaved() {
        AccountId client = insertClient();
        Plan stored = repository.save(Plan.failed(client, Goal.LOSE_FAT, 12));
        entityManager.flush();

        repository.save(stored.edit(Goal.LOSE_FAT, 12, List.of(), LocalDate.parse("2026-11-05")));
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbc.queryForObject(
                        "select automatic_draft from plans where client_id = ?", String.class, client.value()))
                .isNull();
        assertThat(repository.findByClient(client).orElseThrow().getAutomaticDraft()).isEmpty();
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenAPublishedPlanCarriesAMark() {
        AccountId client = insertClient();
        repository.save(Plan.failed(client, Goal.LOSE_FAT, 12));
        entityManager.flush();

        assertThatThrownBy(() -> jdbc.update(
                        "update plans set first_week = '2026-10-26' where client_id = ?", client.value()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldKeepEntryOrderAndAnEmptyLoad_whenReadBack() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        ExerciseId plank = insertExercise();
        ExerciseId row = insertExercise();
        ExerciseEntry loaded = new ExerciseEntry(squat, 4, 5, new BigDecimal("102.5"), 9, 120);
        repository.save(draft(
                client,
                List.of(
                        new Session("Upper body", DayOfWeek.FRIDAY, List.of(entry(row, 3))),
                        new Session("Lower body", DayOfWeek.MONDAY, List.of(loaded, entry(plank, 2), entry(squat, 1))))));
        flushAndClear();

        Plan found = repository.findByClient(client).orElseThrow();

        assertThat(found.getGoal()).isEqualTo(Goal.BUILD_MUSCLE);
        assertThat(found.getWeeks()).isEqualTo(12);
        assertThat(found.isPublished()).isFalse();
        assertThat(found.getSessions()).extracting(Session::day).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        List<ExerciseEntry> entries = found.getSessions().getFirst().entries();
        assertThat(entries).extracting(ExerciseEntry::exercise).containsExactly(squat, plank, squat);
        assertThat(entries.getFirst().loadKg()).isEqualByComparingTo("102.5");
        assertThat(entries.get(1).loadKg()).isNull();
    }

    @Test
    void shouldKeepTheFirstWeek_whenPublished() {
        AccountId client = insertClient();
        LocalDate monday = LocalDate.parse("2026-10-26");
        repository.save(new Plan(PlanId.random(), client, Goal.LOSE_FAT, 8, monday, List.of(), 0));
        flushAndClear();

        Plan found = repository.findByClient(client).orElseThrow();

        assertThat(found.getFirstWeek()).contains(monday);
        assertThat(found.isPublished()).isTrue();
    }

    @Test
    void shouldRefuseAsChanged_whenTheClientAlreadyHasAPlan() {
        AccountId client = insertClient();
        repository.save(draft(client, List.of()));

        assertThatThrownBy(() -> repository.save(draft(client, List.of()))).isInstanceOf(PlanChangedException.class);
    }

    // Hibernate inserts before it deletes, so without the flush the kept day would break the unique constraint
    @Test
    void shouldReplaceTheSessions_whenSavedAgainWithTheSameDays() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        ExerciseId row = insertExercise();
        Plan saved = repository.save(draft(
                client, List.of(new Session("Lower body", DayOfWeek.MONDAY, List.of(entry(squat, 3))))));
        flushAndClear();

        repository.save(withSessions(
                saved,
                List.of(new Session("Pull", DayOfWeek.MONDAY, List.of(entry(row, 4), entry(squat, 2))),
                        new Session("Push", DayOfWeek.THURSDAY, List.of()))));
        flushAndClear();

        Plan found = repository.findByClient(client).orElseThrow();
        assertThat(found.getSessions()).extracting(Session::name).containsExactly("Pull", "Push");
        assertThat(found.getSessions().getFirst().entries())
                .extracting(ExerciseEntry::exercise)
                .containsExactly(row, squat);
        assertThat(jdbc.queryForObject("select count(*) from plan_sessions where plan_id = ?", Long.class, saved.getId().value()))
                .isEqualTo(2);
    }

    @Test
    void shouldRaiseTheVersion_whenOnlyAnEntryChanged() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        Plan saved = repository.save(draft(
                client, List.of(new Session("Lower body", DayOfWeek.MONDAY, List.of(entry(squat, 3))))));
        flushAndClear();

        // loaded with the lock first, as the use case does
        Plan locked = repository.findByClientForUpdate(client).orElseThrow();
        Plan resaved = repository.save(withSessions(
                locked, List.of(new Session("Lower body", DayOfWeek.MONDAY, List.of(entry(squat, 4))))));
        flushAndClear();

        assertThat(resaved.getVersion()).isGreaterThan(saved.getVersion());
        assertThat(repository.findByClient(client).orElseThrow().getVersion()).isEqualTo(resaved.getVersion());
    }

    @Test
    void shouldFindThePlanForUpdate() {
        AccountId client = insertClient();
        Plan saved = repository.save(draft(client, List.of()));
        flushAndClear();

        assertThat(repository.findByClientForUpdate(client))
                .map(Plan::getId)
                .contains(saved.getId());
        assertThat(repository.findByClientForUpdate(AccountId.random())).isEmpty();
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenTheFirstWeekIsNotAMonday() {
        AccountId client = insertClient();
        Plan saved = repository.save(draft(client, List.of()));
        flushAndClear();

        assertThatThrownBy(() -> jdbc.update(
                        "update plans set first_week = date '2026-10-27' where id = ?", saved.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Plan savePublished(AccountId client) {
        return repository.save(new Plan(
                PlanId.random(),
                client,
                Goal.LOSE_FAT,
                8,
                LocalDate.parse("2026-10-26"),
                List.of(new Session("A", DayOfWeek.MONDAY, List.of())),
                0));
    }

    @Test
    void shouldMarkAPublishedPlanSeenOnce_andKeepTheFirstTime() {
        AccountId client = insertClient();
        savePublished(client);
        flushAndClear();
        assertThat(repository.isSeenByClient(client)).isFalse();

        repository.markSeenByClient(client, Instant.parse("2026-11-02T10:00:00Z"));
        repository.markSeenByClient(client, Instant.parse("2026-11-03T10:00:00Z"));
        flushAndClear();

        assertThat(repository.isSeenByClient(client)).isTrue();
        assertThat(jdbc.queryForObject(
                        "select client_seen_at from plans where client_id = ?", Instant.class, client.value()))
                .isEqualTo(Instant.parse("2026-11-02T10:00:00Z"));
    }

    @Test
    void shouldNotMarkADraft() {
        AccountId client = insertClient();
        repository.save(draft(client, List.of()));
        flushAndClear();

        repository.markSeenByClient(client, Instant.parse("2026-11-02T10:00:00Z"));
        flushAndClear();

        assertThat(repository.isSeenByClient(client)).isFalse();
    }

    // the trainer's open editor holds this version, so looking at the plan must not make its next save stale
    @Test
    void shouldNotRaiseTheVersion_whenThePlanIsMarkedSeen() {
        AccountId client = insertClient();
        Plan saved = savePublished(client);
        flushAndClear();

        repository.markSeenByClient(client, Instant.parse("2026-11-02T10:00:00Z"));
        flushAndClear();

        assertThat(repository.findByClient(client).orElseThrow().getVersion()).isEqualTo(saved.getVersion());
    }

    @Test
    void shouldKeepTheMark_whenTheTrainerSavesThePlanAgain() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        Plan saved = savePublished(client);
        flushAndClear();
        repository.markSeenByClient(client, Instant.parse("2026-11-02T10:00:00Z"));
        flushAndClear();

        Plan stored = repository.findByClientForUpdate(client).orElseThrow();
        repository.save(withSessions(
                stored, List.of(new Session("A", DayOfWeek.MONDAY, List.of(entry(squat, 5))))));
        flushAndClear();

        assertThat(repository.isSeenByClient(client)).isTrue();
        assertThat(repository.findByClient(client).orElseThrow().getVersion()).isGreaterThan(saved.getVersion());
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenADraftIsMarkedSeen() {
        AccountId client = insertClient();
        Plan saved = repository.save(draft(client, List.of()));
        flushAndClear();

        assertThatThrownBy(() -> jdbc.update(
                        "update plans set client_seen_at = now() where id = ?", saved.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenTwoSessionsShareADay() {
        AccountId client = insertClient();
        Plan saved = repository.save(draft(client, List.of(new Session("A", DayOfWeek.MONDAY, List.of()))));
        flushAndClear();

        assertThatThrownBy(() -> jdbc.update(
                        "insert into plan_sessions (id, plan_id, name, day_of_week) values (?, ?, 'B', 'MONDAY')",
                        UUID.randomUUID(),
                        saved.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
