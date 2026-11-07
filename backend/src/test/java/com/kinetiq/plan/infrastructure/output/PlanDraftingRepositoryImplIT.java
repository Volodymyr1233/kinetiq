package com.kinetiq.plan.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.DraftingKind;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import({TestcontainersConfiguration.class, PlanDraftingRepositoryImpl.class})
class PlanDraftingRepositoryImplIT {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    @Autowired
    private PlanDraftingRepositoryImpl repository;

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

    @Test
    void shouldTellTheKindOfTheRow_whenRemoving() {
        AccountId first = insertClient();
        AccountId again = insertClient();
        repository.start(first, NOW, DraftingKind.FIRST);
        repository.start(again, NOW, DraftingKind.REBUILD);

        assertThat(repository.remove(first)).contains(DraftingKind.FIRST);
        assertThat(repository.remove(again)).contains(DraftingKind.REBUILD);
        assertThat(repository.remove(first)).isEmpty();
    }

    @Test
    void shouldTellWhetherARowExists() {
        AccountId client = insertClient();
        assertThat(repository.exists(client)).isFalse();

        repository.start(client, NOW, DraftingKind.REBUILD);

        assertThat(repository.exists(client)).isTrue();
    }

    @Test
    void shouldFindOnlyRowsStartedBeforeTheInstant() {
        AccountId older = insertClient();
        AccountId atTheInstant = insertClient();
        AccountId newer = insertClient();
        repository.start(older, NOW.minusSeconds(1), DraftingKind.FIRST);
        repository.start(atTheInstant, NOW, DraftingKind.FIRST);
        repository.start(newer, NOW.plusSeconds(1), DraftingKind.REBUILD);

        assertThat(repository.startedBefore(NOW)).contains(older).doesNotContain(atTheInstant, newer);
    }
}
