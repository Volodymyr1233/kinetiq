package com.kinetiq.sessionlog.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.sessionlog.domain.exception.SessionLogChangedException;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLog;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.model.SetValues;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
@Import({TestcontainersConfiguration.class, SessionLogRepositoryImpl.class, SessionLogMapper.class})
class SessionLogRepositoryImplIT {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
    private static final LocalDate DATE = LocalDate.parse("2026-11-07");

    @Autowired
    private SessionLogRepositoryImpl repository;

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

    private SessionLog start(AccountId client, LocalDate date, List<LoggedEntry> entries) {
        return SessionLog.start(client, date, "Lower body", entries, NOW);
    }

    // cleared, so what is read back comes from the database and not from Hibernate's cached entity
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldKeepTheEntriesInOrderAndAnEmptyLoad_whenReadBack() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        ExerciseId plank = insertExercise();
        SessionLog saved = repository.save(start(
                client,
                DATE,
                List.of(
                        new LoggedEntry(squat, 3, 5, new BigDecimal("100.0"), 8, 120),
                        new LoggedEntry(plank, 2, 45, null, 7, 60),
                        new LoggedEntry(squat, 1, 8, new BigDecimal("60.0"), 7, 90))));
        flushAndClear();

        SessionLog found = repository.findByIdAndClient(saved.getId(), client).orElseThrow();

        assertThat(found.getName()).isEqualTo("Lower body");
        assertThat(found.getDate()).isEqualTo(DATE);
        assertThat(found.getStartedAt()).isEqualTo(NOW);
        assertThat(found.getFinishedAt()).isEmpty();
        assertThat(found.getEntries()).extracting(LoggedEntry::exercise).containsExactly(squat, plank, squat);
        assertThat(found.getEntries().getFirst().loadKg()).isEqualByComparingTo("100.0");
        assertThat(found.getEntries().get(1).loadKg()).isNull();
    }

    @Test
    void shouldFindTheSessionOfADate_andNotOfAnotherClient() {
        AccountId client = insertClient();
        AccountId other = insertClient();
        ExerciseId squat = insertExercise();
        SessionLog saved = repository.save(start(client, DATE, List.of(new LoggedEntry(squat, 3, 5, null, 8, 120))));
        flushAndClear();

        assertThat(repository.findByClientAndDate(client, DATE)).map(SessionLog::getId).contains(saved.getId());
        assertThat(repository.findByClientAndDate(client, DATE.plusDays(1))).isEmpty();
        assertThat(repository.findByIdAndClient(saved.getId(), other)).isEmpty();
        assertThat(repository.findByIdAndClient(SessionLogId.random(), client)).isEmpty();
    }

    @Test
    void shouldRefuseASecondSessionOfTheSameClientAndDate() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        List<LoggedEntry> entries = List.of(new LoggedEntry(squat, 3, 5, null, 8, 120));
        repository.save(start(client, DATE, entries));
        flushAndClear();

        assertThatThrownBy(() -> repository.save(start(client, DATE, entries)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldStoreASetLogWithItsTargetsAndFindItsSession() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        SessionLog log = start(client, DATE, List.of(new LoggedEntry(squat, 3, 5, new BigDecimal("100.0"), 8, 120)));
        SetLogId id = new SetLogId(UUID.randomUUID());
        log.logSet(id, 0, 2, new SetValues(new BigDecimal("102.5"), 4, 9), NOW);
        SessionLog saved = repository.save(log);
        flushAndClear();

        SessionLog found = repository.findByIdAndClient(saved.getId(), client).orElseThrow();

        SetLog stored = found.getSetLogs().getFirst();
        assertThat(stored.id()).isEqualTo(id);
        assertThat(stored.entryPosition()).isZero();
        assertThat(stored.setNumber()).isEqualTo(2);
        assertThat(stored.values().loadKg()).isEqualByComparingTo("102.5");
        assertThat(stored.values().amount()).isEqualTo(4);
        assertThat(stored.targetLoadKg()).isEqualByComparingTo("100.0");
        assertThat(stored.targetAmount()).isEqualTo(5);
        assertThat(stored.targetRpe()).isEqualTo(8);
        assertThat(repository.sessionOfSetLog(id)).contains(saved.getId());
        assertThat(repository.sessionOfSetLog(new SetLogId(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void shouldReplaceTheValuesOfASetLogAndKeepOneRow_whenItIsChanged() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        SessionLog log = start(client, DATE, List.of(new LoggedEntry(squat, 3, 5, new BigDecimal("100.0"), 8, 120)));
        SetLogId id = new SetLogId(UUID.randomUUID());
        log.logSet(id, 0, 1, new SetValues(new BigDecimal("100.0"), 5, 8), NOW);
        repository.save(log);
        flushAndClear();

        SessionLog stored = repository.findByIdAndClient(log.getId(), client).orElseThrow();
        stored.logSet(id, 0, 1, new SetValues(null, 6, 9), NOW.plusSeconds(60));
        repository.save(stored);
        flushAndClear();

        SessionLog found = repository.findByIdAndClient(log.getId(), client).orElseThrow();
        assertThat(found.getSetLogs()).hasSize(1);
        SetLog changed = found.getSetLogs().getFirst();
        assertThat(changed.values().loadKg()).isNull();
        assertThat(changed.values().amount()).isEqualTo(6);
        assertThat(changed.loggedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(changed.targetLoadKg()).isEqualByComparingTo("100.0");
    }

    @Test
    void shouldReadTheHeadersOfTheRangeForTheClientOnly_bothBoundsIncluded() {
        AccountId client = insertClient();
        AccountId other = insertClient();
        ExerciseId squat = insertExercise();
        List<LoggedEntry> entries = List.of(new LoggedEntry(squat, 3, 5, new BigDecimal("100.0"), 8, 120));
        SessionLog first = repository.save(start(client, DATE, entries));
        SessionLog last = repository.save(start(client, DATE.plusDays(6), entries));
        repository.save(start(client, DATE.minusDays(1), entries));
        repository.save(start(client, DATE.plusDays(7), entries));
        repository.save(start(other, DATE.plusDays(2), entries));
        flushAndClear();

        List<SessionLogHeader> headers = repository.headersBetween(client, DATE, DATE.plusDays(6));

        assertThat(headers).extracting(SessionLogHeader::id).containsExactlyInAnyOrder(first.getId(), last.getId());
        assertThat(headers.getFirst().startedAt()).isEqualTo(NOW);
        assertThat(headers.getFirst().finished()).isEmpty();
    }

    @Test
    void shouldStoreTheFinishTime() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        SessionLog log = start(client, DATE, List.of(new LoggedEntry(squat, 3, 5, new BigDecimal("100.0"), 8, 120)));
        log.logSet(new SetLogId(UUID.randomUUID()), 0, 1, new SetValues(new BigDecimal("100.0"), 5, 8), NOW);
        repository.save(log);
        flushAndClear();

        SessionLog stored = repository.findByIdAndClient(log.getId(), client).orElseThrow();
        stored.finish(NOW.plusSeconds(600));
        repository.save(stored);
        flushAndClear();

        assertThat(repository.findByIdAndClient(log.getId(), client).orElseThrow().getFinishedAt())
                .contains(NOW.plusSeconds(600));
        assertThat(repository.headersBetween(client, DATE, DATE).getFirst().finished())
                .contains(NOW.plusSeconds(600));
    }

    @Test
    void shouldStoreTheNote_andClearIt() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        SessionLog log = start(client, DATE, List.of(new LoggedEntry(squat, 3, 5, new BigDecimal("100.0"), 8, 120)));
        log.logSet(new SetLogId(UUID.randomUUID()), 0, 1, new SetValues(new BigDecimal("100.0"), 5, 8), NOW);
        log.finish(NOW.plusSeconds(60));
        log.writeNote("Felt strong", NOW.plusSeconds(60));
        repository.save(log);
        flushAndClear();

        SessionLog stored = repository.findByIdAndClient(log.getId(), client).orElseThrow();
        assertThat(stored.getNote()).contains("Felt strong");

        stored.writeNote("", NOW.plusSeconds(120));
        repository.save(stored);
        flushAndClear();

        assertThat(repository.findByIdAndClient(log.getId(), client).orElseThrow().getNote()).isEmpty();
    }

    @Test
    void shouldRefuseAsChanged_whenSavedFromAStaleCopy() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        SessionLog log = start(client, DATE, List.of(new LoggedEntry(squat, 3, 5, new BigDecimal("100.0"), 8, 120)));
        log.logSet(new SetLogId(UUID.randomUUID()), 0, 1, new SetValues(new BigDecimal("100.0"), 5, 8), NOW);
        repository.save(log);
        flushAndClear();
        SessionLog stale = repository.findByIdAndClient(log.getId(), client).orElseThrow();
        SessionLog current = repository.findByIdAndClient(log.getId(), client).orElseThrow();
        current.finish(NOW.plusSeconds(60));
        repository.save(current);
        flushAndClear();

        stale.finish(NOW.plusSeconds(120));

        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(SessionLogChangedException.class);
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenASetHasNoRepetitions() {
        assertThat(insertSetLogWith(0, 8)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenASetHasAnRpeOfFour() {
        assertThat(insertSetLogWith(5, 4)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Throwable insertSetLogWith(int amount, int rpe) {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        SessionLog saved = repository.save(start(client, DATE, List.of(new LoggedEntry(squat, 3, 5, null, 8, 120))));
        flushAndClear();

        String insert =
                """
                insert into set_logs (id, session_log_id, entry_position, set_number, load_kg, amount, rpe,
                                      target_load_kg, target_amount, target_rpe, logged_at, created_at, updated_at,
                                      version)
                values (?, ?, 0, 1, null, ?, ?, null, 5, 8, now(), now(), now(), 0)
                """;
        return catchThrowable(() -> jdbc.update(insert, UUID.randomUUID(), saved.getId().value(), amount, rpe));
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenTwoSetLogsTakeOnePlace() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        SessionLog saved = repository.save(start(client, DATE, List.of(new LoggedEntry(squat, 3, 5, null, 8, 120))));
        flushAndClear();

        String insert =
                """
                insert into set_logs (id, session_log_id, entry_position, set_number, load_kg, amount, rpe,
                                      target_load_kg, target_amount, target_rpe, logged_at, created_at, updated_at,
                                      version)
                values (?, ?, 0, 1, null, 5, 8, null, 5, 8, now(), now(), now(), 0)
                """;
        jdbc.update(insert, UUID.randomUUID(), saved.getId().value());

        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), saved.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
