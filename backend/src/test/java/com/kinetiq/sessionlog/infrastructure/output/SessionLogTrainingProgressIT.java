package com.kinetiq.sessionlog.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.model.SetValues;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// accounts and exercises are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({
    TestcontainersConfiguration.class,
    SessionLogTrainingProgress.class,
    SessionLogRepositoryImpl.class,
    SessionLogMapper.class
})
class SessionLogTrainingProgressIT {

    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");
    private static final LocalDate DATE = LocalDate.parse("2026-11-08");

    @Autowired
    private SessionLogTrainingProgress progress;

    @Autowired
    private SessionLogRepositoryImpl logs;

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

    private SessionLog startSession(AccountId client) {
        UUID exercise = UUID.randomUUID();
        jdbc.update(
                """
                insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount,
                                       rest_seconds, cues, created_at, updated_at, version)
                values (?, null, ?, 'LEGS', '', 'REPS', 3, 10, 90, '', now(), now(), 0)
                """,
                exercise,
                "Exercise " + exercise);
        return logs.save(SessionLog.start(
                client,
                DATE,
                "Lower body",
                List.of(new LoggedEntry(new ExerciseId(exercise), 3, 5, null, 8, 120)),
                NOW));
    }

    @Test
    void shouldBeFalse_whenNoSessionWasStarted() {
        assertThat(progress.hasLoggedSet(insertClient(), DATE)).isFalse();
    }

    @Test
    void shouldBeFalse_whenTheSessionWasStartedWithoutASet() {
        AccountId client = insertClient();
        startSession(client);

        assertThat(progress.hasLoggedSet(client, DATE)).isFalse();
    }

    @Test
    void shouldBeTrueOnlyForThatClientAndDate_afterOneSetIsLogged() {
        AccountId client = insertClient();
        SessionLog log = startSession(client);
        log.logSet(new SetLogId(UUID.randomUUID()), 0, 1, new SetValues(null, 5, 8), NOW);
        logs.save(log);

        assertThat(progress.hasLoggedSet(client, DATE)).isTrue();
        assertThat(progress.hasLoggedSet(client, DATE.plusDays(1))).isFalse();
        assertThat(progress.hasLoggedSet(insertClient(), DATE)).isFalse();
    }
}
