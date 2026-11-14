package com.kinetiq.notification.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.NoticeKind;
import com.kinetiq.notification.domain.model.Notification;
import com.kinetiq.notification.domain.model.NotificationId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// accounts are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, NotificationRepositoryImpl.class})
class NotificationRepositoryImplIT {

    @Autowired
    private NotificationRepositoryImpl repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId insertAccount(String role, UUID trainer) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Person', ?, 'hash', ?, ?, 'Europe/Warsaw', true, ?, now(), now(), 0)
                """,
                id,
                id + "@example.com",
                role,
                trainer,
                trainer == null ? null : java.sql.Timestamp.from(Instant.now()));
        return new AccountId(id);
    }

    private Notification notification(AccountId recipient, NoticeKind kind, AccountId client, Instant at) {
        return new Notification(new NotificationId(UUID.randomUUID()), recipient, kind, client, at, null);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldReadTheLatestOfOnePerson_newestFirst_andCountTheUnreadOnes() {
        AccountId trainer = insertAccount("TRAINER", null);
        AccountId other = insertAccount("TRAINER", null);
        Notification older = repository.save(notification(trainer, NoticeKind.DRAFT_TO_REVIEW, null, Instant.parse("2026-11-14T08:00:00Z")));
        Notification newer = repository.save(notification(trainer, NoticeKind.RECOVERY_DAY, null, Instant.parse("2026-11-14T09:00:00Z")));
        repository.save(notification(other, NoticeKind.DRAFT_TO_REVIEW, null, Instant.parse("2026-11-14T10:00:00Z")));
        flushAndClear();

        assertThat(repository.latest(trainer, 30)).extracting(Notification::id).containsExactly(newer.id(), older.id());
        assertThat(repository.latest(trainer, 1)).hasSize(1);
        assertThat(repository.countUnread(trainer)).isEqualTo(2);
    }

    @Test
    void shouldFindANotificationOnlyForItsRecipient() {
        AccountId trainer = insertAccount("TRAINER", null);
        AccountId other = insertAccount("TRAINER", null);
        Notification stored = repository.save(notification(trainer, NoticeKind.DRAFT_TO_REVIEW, null, Instant.now()));
        flushAndClear();

        assertThat(repository.findOf(trainer, stored.id())).isPresent();
        assertThat(repository.findOf(other, stored.id())).isEmpty();
    }

    @Test
    void shouldKeepTheReadMoment() {
        AccountId trainer = insertAccount("TRAINER", null);
        Notification stored = repository.save(notification(trainer, NoticeKind.DRAFT_TO_REVIEW, null, Instant.now()));
        Instant read = Instant.parse("2026-11-14T12:00:00Z");
        repository.save(stored.readAt(read));
        flushAndClear();

        assertThat(repository.findOf(trainer, stored.id()).orElseThrow().readAt()).isEqualTo(read);
        assertThat(repository.countUnread(trainer)).isZero();
    }

    @Test
    void shouldEraseWhatWasTold_andWhatWasToldAbout_whenAnAccountIsDeleted() {
        AccountId trainer = insertAccount("TRAINER", null);
        AccountId client = insertAccount("CLIENT", trainer.value());
        repository.save(notification(client, NoticeKind.PLAN_PUBLISHED, null, Instant.now()));
        repository.save(notification(trainer, NoticeKind.RECOVERY_DAY, client, Instant.now()));
        Notification forgotten = repository.save(notification(trainer, NoticeKind.CLIENT_DELETED, null, Instant.now()));
        flushAndClear();

        jdbc.update("delete from accounts where id = ?", client.value());

        assertThat(jdbc.queryForObject("select count(*) from notifications", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select id from notifications", UUID.class)).isEqualTo(forgotten.id().value());
    }
}
