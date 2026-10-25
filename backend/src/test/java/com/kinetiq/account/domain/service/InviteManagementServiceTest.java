package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.InviteAccountExistsException;
import com.kinetiq.account.domain.exception.InviteNotFoundException;
import com.kinetiq.account.domain.input.CreateInviteCommand;
import com.kinetiq.account.domain.input.CreatedInvite;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.account.domain.model.InviteStatus;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.output.Mail;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InviteManagementServiceTest {

    private static final String APP_URL = "http://localhost:5173";
    private static final String LINK_PREFIX = APP_URL + "/join#token=";
    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final Email JULIA = new Email("julia@example.com");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryInviteRepository invites = new InMemoryInviteRepository();
    private final RecordingMailSender mails = new RecordingMailSender();
    private final MutableClock clock = new MutableClock(NOW);
    private final InviteManagementService service =
            new InviteManagementService(invites, accounts, mails, clock, URI.create(APP_URL));

    private AccountId trainer;
    private AccountId otherTrainer;

    @BeforeEach
    void addTrainers() {
        trainer = accounts.add(trainer("Marek Nowak", "marek@example.com")).getId();
        otherTrainer = accounts.add(trainer("Anna Kowalska", "anna@example.com")).getId();
    }

    private static Account trainer(String name, String email) {
        return Account.signUpTrainer(name, new Email(email), "hash", ZoneId.of("Europe/Warsaw"), Language.EN);
    }

    private CreatedInvite invite(AccountId trainerId, Email email) {
        return service.create(new CreateInviteCommand(trainerId, email, Locale.forLanguageTag("pl")));
    }

    private static SecretToken tokenOf(String link) {
        assertThat(link).startsWith(LINK_PREFIX);
        return new SecretToken(link.substring(LINK_PREFIX.length()));
    }

    @Nested
    class Create {

        @Test
        void shouldEmailOneLinkAndReturnAnother_whenEmailHasNoAccount() {
            CreatedInvite created = invite(trainer, JULIA);

            Invite stored = invites.findById(created.invite().getId()).orElseThrow();
            assertThat(stored.isOpen(NOW)).isTrue();
            assertThat(stored.getTrainerId()).isEqualTo(trainer);
            assertThat(mails.sent).hasSize(1);
            Mail mail = mails.sent.getFirst();
            assertThat(mail.to()).isEqualTo(JULIA);
            assertThat(mail.template()).isEqualTo("invite");
            assertThat(mail.locale()).isEqualTo(Locale.forLanguageTag("pl"));
            assertThat(mail.values())
                    .containsEntry("trainerName", "Marek Nowak")
                    .containsEntry("validDays", "7");
            assertThat(tokenOf(mail.values().get("link")).hash()).isEqualTo(stored.getEmailTokenHash());
            assertThat(tokenOf(created.link()).hash()).isEqualTo(stored.getLinkTokenHash());
        }

        @ParameterizedTest
        @ValueSource(strings = {"TRAINER", "CLIENT"})
        void shouldRefuseAndSendNothing_whenEmailHasAccountOfAnyRoleConfirmedOrNot(Role role) {
            accounts.add(Account.restore(
                    AccountId.random(),
                    "Ola Dąbrowska",
                    new Email("ola@example.com"),
                    "hash",
                    role,
                    ZoneId.of("Europe/Warsaw"),
                    AccountSettings.startingWith(Language.EN),
                    role == Role.CLIENT,
                    null,
                    null));

            assertThatThrownBy(() -> invite(trainer, new Email("OLA@Example.com")))
                    .isInstanceOf(InviteAccountExistsException.class);
            assertThat(invites.byId).isEmpty();
            assertThat(mails.sent).isEmpty();
        }

        @Test
        void shouldRefuse_whenTrainerInvitesOwnEmail() {
            assertThatThrownBy(() -> invite(trainer, new Email("marek@example.com")))
                    .isInstanceOf(InviteAccountExistsException.class);
        }

        @Test
        void shouldReplaceEarlierInviteAndEmailAgain_whenSameTrainerInvitesSameEmail() {
            Invite first = invite(trainer, JULIA).invite();
            clock.advance(Duration.ofHours(1));

            Invite second = invite(trainer, JULIA).invite();

            assertThat(invites.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(InviteStatus.REVOKED);
            assertThat(second.isOpen(clock.instant())).isTrue();
            assertThat(mails.sent).hasSize(2);
        }

        @Test
        void shouldReplaceEarlierInvite_whenItHasExpired() {
            Invite first = invite(trainer, JULIA).invite();
            clock.advance(Duration.ofDays(8));

            invite(trainer, JULIA);

            assertThat(invites.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(InviteStatus.REVOKED);
        }

        @Test
        void shouldLeaveOtherTrainersInvite_whenSameEmailIsInvited() {
            Invite others = invite(otherTrainer, JULIA).invite();

            invite(trainer, JULIA);

            assertThat(invites.findById(others.getId()).orElseThrow().isOpen(NOW)).isTrue();
        }
    }

    @Nested
    class ListOpen {

        @Test
        void shouldListOwnOpenInvitesNewestFirst() {
            Invite older = invite(trainer, new Email("adam@example.com")).invite();
            clock.advance(Duration.ofHours(1));
            Invite newer = invite(trainer, JULIA).invite();
            Invite revoked = invite(trainer, new Email("piotr@example.com")).invite();
            service.revoke(trainer, revoked.getId());
            invite(otherTrainer, new Email("ewa@example.com"));

            assertThat(service.listOpen(trainer)).extracting(Invite::getId).containsExactly(newer.getId(), older.getId());
        }

        @Test
        void shouldLeaveOutExpiredInvites() {
            invite(trainer, JULIA);
            clock.advance(Duration.ofDays(7));

            assertThat(service.listOpen(trainer)).isEmpty();
        }
    }

    @Nested
    class ReplaceLink {

        @Test
        void shouldReturnNewLinkAndKeepEmailedOne_whenInviteIsOwnAndOpen() {
            CreatedInvite created = invite(trainer, JULIA);
            String emailed = mails.sent.getFirst().values().get("link");

            String link = service.replaceLink(trainer, created.invite().getId());

            Invite stored = invites.findById(created.invite().getId()).orElseThrow();
            assertThat(tokenOf(link).hash()).isEqualTo(stored.getLinkTokenHash());
            assertThat(tokenOf(created.link()).hash()).isNotEqualTo(stored.getLinkTokenHash());
            assertThat(tokenOf(emailed).hash()).isEqualTo(stored.getEmailTokenHash());
        }
    }

    @Nested
    class Revoke {

        @Test
        void shouldRevoke_whenInviteIsOwnAndOpen() {
            Invite invite = invite(trainer, JULIA).invite();

            service.revoke(trainer, invite.getId());

            assertThat(invites.findById(invite.getId()).orElseThrow().getStatus()).isEqualTo(InviteStatus.REVOKED);
        }
    }

    // both operations answer the same for everything that is not the trainer's open invite
    @Nested
    class NotFound {

        private void assertNotFound(BiConsumer<AccountId, InviteId> operation, AccountId trainerId, InviteId id) {
            Invite before = invites.byId.get(id);
            String linkHash = before == null ? null : before.getLinkTokenHash();
            InviteStatus status = before == null ? null : before.getStatus();

            assertThatThrownBy(() -> operation.accept(trainerId, id)).isInstanceOf(InviteNotFoundException.class);
            if (before != null) {
                assertThat(before.getLinkTokenHash()).isEqualTo(linkHash);
                assertThat(before.getStatus()).isEqualTo(status);
            }
        }

        private void assertBothNotFound(AccountId trainerId, InviteId id) {
            assertNotFound(service::replaceLink, trainerId, id);
            assertNotFound(service::revoke, trainerId, id);
        }

        @Test
        void shouldAnswerNotFound_whenInviteIsAnotherTrainers() {
            Invite others = invite(otherTrainer, JULIA).invite();

            assertBothNotFound(trainer, others.getId());
        }

        @Test
        void shouldAnswerNotFound_whenInviteIsUnknown() {
            assertBothNotFound(trainer, InviteId.random());
        }

        @Test
        void shouldAnswerNotFound_whenInviteIsRevoked() {
            Invite invite = invite(trainer, JULIA).invite();
            service.revoke(trainer, invite.getId());

            assertBothNotFound(trainer, invite.getId());
        }

        @Test
        void shouldAnswerNotFound_whenInviteIsUsed() {
            Invite invite = invite(trainer, JULIA).invite();
            invite.use(NOW);

            assertBothNotFound(trainer, invite.getId());
        }

        @Test
        void shouldAnswerNotFound_whenInviteHasExpired() {
            Invite invite = invite(trainer, JULIA).invite();
            clock.advance(Duration.ofDays(7));

            assertBothNotFound(trainer, invite.getId());
        }
    }
}
