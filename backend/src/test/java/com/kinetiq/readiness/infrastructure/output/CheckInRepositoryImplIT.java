package com.kinetiq.readiness.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.CheckIn;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// accounts are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, CheckInRepositoryImpl.class, CheckInMapper.class})
class CheckInRepositoryImplIT {

    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");
    private static final LocalDate DATE = LocalDate.parse("2026-11-08");

    @Autowired
    private CheckInRepositoryImpl repository;

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

    // cleared, so what is read back comes from the database and not from Hibernate's cached entity
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private int rows() {
        return jdbc.queryForObject("select count(*) from check_ins", Integer.class);
    }

    @Test
    void shouldReadTheCheckInBack_byClientAndDate() {
        AccountId client = insertClient();
        CheckIn saved = repository.save(CheckIn.give(client, DATE, new Answers(4, 2, 1), NOW));
        flushAndClear();

        CheckIn read = repository.findByClientAndDate(client, DATE).orElseThrow();

        assertThat(read.getId()).isEqualTo(saved.getId());
        assertThat(read.getAnswers()).isEqualTo(new Answers(4, 2, 1));
        assertThat(read.getSavedAt()).isEqualTo(NOW);
        assertThat(repository.findByClientAndDate(client, DATE.minusDays(1))).isEmpty();
        assertThat(repository.findByClientAndDate(insertClient(), DATE)).isEmpty();
    }

    @Test
    void shouldKeepOneRow_whenTheCheckInIsChanged() {
        AccountId client = insertClient();
        repository.save(CheckIn.give(client, DATE, new Answers(4, 2, 1), NOW));
        flushAndClear();
        CheckIn stored = repository.findByClientAndDate(client, DATE).orElseThrow();
        stored.change(new Answers(2, 4, 3), NOW.plusSeconds(60));

        repository.save(stored);
        flushAndClear();

        assertThat(rows()).isEqualTo(1);
        CheckIn read = repository.findByClientAndDate(client, DATE).orElseThrow();
        assertThat(read.getAnswers()).isEqualTo(new Answers(2, 4, 3));
        assertThat(read.getSavedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void shouldRefuseASecondCheckIn_forTheSameClientAndDate() {
        AccountId client = insertClient();
        repository.save(CheckIn.give(client, DATE, new Answers(4, 2, 1), NOW));

        assertThatThrownBy(() -> repository.save(CheckIn.give(client, DATE, new Answers(3, 3, 3), NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void shouldRefuseAnAnswerOutsideOneToFive_inTheDatabase(int answer) {
        AccountId client = insertClient();

        assertThatThrownBy(() -> jdbc.update(
                        """
                        insert into check_ins (id, client_id, check_in_date, sleep, soreness, stress, saved_at,
                                               created_at, updated_at, version)
                        values (?, ?, ?, ?, 3, 3, now(), now(), now(), 0)
                        """,
                        UUID.randomUUID(),
                        client.value(),
                        DATE,
                        answer))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
