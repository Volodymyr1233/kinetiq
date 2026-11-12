package com.kinetiq.account.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.account.domain.model.SentInvite;
import com.kinetiq.common.domain.model.Email;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** The selection of what the retention sweep erases, at each boundary of ADR 0005. */
@DataJpaTest
@Import({
    TestcontainersConfiguration.class,
    AccountRepositoryImpl.class,
    AccountMapperImpl.class,
    InviteRepositoryImpl.class,
    InviteMapperImpl.class
})
class RetentionIT {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final Instant LINK_CUT_OFF = NOW.minus(Duration.ofDays(6));
    private static final Instant INVITE_CUT_OFF = NOW.minus(Duration.ofHours(1));

    @Autowired
    private AccountRepositoryImpl accounts;

    @Autowired
    private InviteRepositoryImpl invites;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private Account trainer(String email, Instant linkExpiresAt, boolean confirmed) {
        Account account = Account.signUpTrainer("Marek", new Email(email), "hash", ZoneId.of("UTC"), Language.EN);
        // a link expires 24 hours after it was sent
        account.issueConfirmation(SecretToken.generate(), linkExpiresAt.minus(Duration.ofHours(24)));
        if (confirmed) {
            account.confirmEmail(linkExpiresAt.minus(Duration.ofHours(1)));
        }
        accounts.save(account);
        return account;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    class UnconfirmedTrainers {

        @Test
        void shouldEraseOnlyTrainersWhoseLinkExpiredMoreThanSixDaysAgo() {
            Account old = trainer("old@example.com", LINK_CUT_OFF.minusSeconds(60), false);
            Account recent = trainer("recent@example.com", LINK_CUT_OFF.plus(Duration.ofHours(1)), false);
            Account confirmed = trainer("confirmed@example.com", LINK_CUT_OFF.minus(Duration.ofDays(30)), true);
            flushAndClear();

            int erased = accounts.deleteUnconfirmedTrainers(LINK_CUT_OFF);

            assertThat(erased).isEqualTo(1);
            assertThat(accounts.findById(old.getId())).isEmpty();
            assertThat(accounts.findById(recent.getId())).isPresent();
            assertThat(accounts.findById(confirmed.getId())).isPresent();
        }

        @Test
        void shouldKeepAnUnconfirmedClient_whateverTheAgeOfTheLink() {
            Account trainer = trainer("marek@example.com", NOW, true);
            Account client = Account.restore(
                    AccountId.random(),
                    "Anna",
                    new Email("anna@example.com"),
                    "hash",
                    Role.CLIENT,
                    trainer.getId(),
                    ZoneId.of("UTC"),
                    AccountSettings.startingWith(Language.EN),
                    false,
                    null,
                    null,
                    null,
                    NOW.minus(Duration.ofDays(60)));
            client.issueConfirmation(SecretToken.generate(), NOW.minus(Duration.ofDays(60)));
            accounts.save(client);
            flushAndClear();

            accounts.deleteUnconfirmedTrainers(LINK_CUT_OFF);

            assertThat(accounts.findById(client.getId())).isPresent();
        }
    }

    @Nested
    class EndedInvites {

        private AccountId trainerId;

        @BeforeEach
        void storeTrainer() {
            trainerId = trainer("marek@example.com", NOW, true).getId();
        }

        private SentInvite send(String email, Instant sentAt) {
            SentInvite sent = Invite.send(trainerId, new Email(email), sentAt);
            invites.save(sent.invite());
            return sent;
        }

        private Invite ended(String email, boolean used, Instant endedAt) {
            Invite invite = send(email, NOW.minus(Duration.ofDays(1))).invite();
            if (used) {
                invite.use(endedAt);
            } else {
                invite.revoke(endedAt);
            }
            invites.save(invite);
            // the database stamps the change with the current time, so the test moves it to when the invite ended
            jdbc.update(
                    "update invites set updated_at = ? where id = ?", Timestamp.from(endedAt), invite.getId().value());
            return invite;
        }

        @Test
        void shouldEraseUsedAndRevokedInvites_whenTheyEndedMoreThanAnHourAgo() {
            Invite usedOld = ended("a@example.com", true, INVITE_CUT_OFF.minusSeconds(60));
            Invite revokedOld = ended("b@example.com", false, INVITE_CUT_OFF.minusSeconds(60));
            Invite usedRecent = ended("c@example.com", true, INVITE_CUT_OFF.plusSeconds(60));
            Invite revokedRecent = ended("d@example.com", false, INVITE_CUT_OFF.plusSeconds(60));
            flushAndClear();

            int erased = invites.deleteEnded(INVITE_CUT_OFF);

            assertThat(erased).isEqualTo(2);
            assertThat(invites.findById(usedOld.getId())).isEmpty();
            assertThat(invites.findById(revokedOld.getId())).isEmpty();
            assertThat(invites.findById(usedRecent.getId())).isPresent();
            assertThat(invites.findById(revokedRecent.getId())).isPresent();
        }

        @Test
        void shouldEraseOpenInvites_whenTheyExpiredMoreThanAnHourAgo() {
            Instant expiredLongAgo = INVITE_CUT_OFF.minusSeconds(60).minus(Invite.VALIDITY);
            Instant expiredJustNow = INVITE_CUT_OFF.plusSeconds(60).minus(Invite.VALIDITY);
            SentInvite old = send("a@example.com", expiredLongAgo);
            SentInvite recent = send("b@example.com", expiredJustNow);
            SentInvite open = send("c@example.com", NOW);
            flushAndClear();

            invites.deleteEnded(INVITE_CUT_OFF);

            assertThat(invites.findById(old.invite().getId())).isEmpty();
            assertThat(invites.findById(recent.invite().getId())).isPresent();
            assertThat(invites.findById(open.invite().getId())).isPresent();
        }

        @Test
        void shouldFindNoInviteByAnErasedSecret_soItsLinkNoLongerWorks() {
            SentInvite old = send("a@example.com", INVITE_CUT_OFF.minusSeconds(60).minus(Invite.VALIDITY));
            flushAndClear();

            invites.deleteEnded(INVITE_CUT_OFF);

            assertThat(invites.findByTokenHash(old.emailToken().hash())).isEmpty();
            assertThat(invites.findByTokenHash(old.linkToken().hash())).isEmpty();
        }
    }
}
