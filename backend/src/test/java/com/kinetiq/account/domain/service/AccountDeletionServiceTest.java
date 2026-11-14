package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.PasswordIncorrectException;
import com.kinetiq.account.domain.exception.SignInLockedException;
import com.kinetiq.account.domain.exception.TrainerHasClientsException;
import com.kinetiq.account.domain.input.DeletionCheck;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.common.domain.model.NoticeKind;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AccountDeletionServiceTest {

    private static final String PASSWORD = "Kinetiq-2026";
    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryInviteRepository invites = new InMemoryInviteRepository();
    private final RecordingPasswordHasher hasher = new RecordingPasswordHasher();
    private final FakeFailedSignInsRepository failedSignIns = new FakeFailedSignInsRepository();
    private final RecordingAccountSessions sessions = new RecordingAccountSessions();
    private final List<Notice> notices = new ArrayList<>();
    private final AccountDeletionService service =
            new AccountDeletionService(
                    accounts, invites, hasher, failedSignIns, sessions, notices::add, new MutableClock(NOW));

    private Account account(String name, String email, Role role, AccountId trainer) {
        return accounts.add(Account.restore(
                AccountId.random(),
                name,
                new Email(email),
                "hashed:" + PASSWORD,
                role,
                trainer,
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
                null,
                role == Role.CLIENT ? Instant.EPOCH : null));
    }

    private Account trainer(String email) {
        return account("Marek Nowak", email, Role.TRAINER, null);
    }

    private Account client(String email, Account trainer) {
        return account("Anna Kowalska", email, Role.CLIENT, trainer.getId());
    }

    private Invite invite(Account trainer, String email) {
        Invite invite = Invite.send(trainer.getId(), new Email(email), NOW).invite();
        invites.save(invite);
        return invite;
    }

    @Nested
    class Check {

        @Test
        void shouldAnswerTheNumberOfClients_whenTheAccountIsATrainers() {
            Account marek = trainer("marek@example.com");
            client("anna@example.com", marek);
            client("ola@example.com", marek);

            assertThat(service.check(marek.getId())).isEqualTo(new DeletionCheck(2, null));
        }

        @Test
        void shouldAnswerTheTrainersName_whenTheAccountIsAClients() {
            Account anna = client("anna@example.com", trainer("marek@example.com"));

            assertThat(service.check(anna.getId())).isEqualTo(new DeletionCheck(0, "Marek Nowak"));
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteAClientWithTheirTrainersInvitesForThem_andEndEverySignIn() {
            Account marek = trainer("marek@example.com");
            Account other = trainer("other@example.com");
            Account anna = client("anna@example.com", marek);
            Invite own = invite(marek, "anna@example.com");
            Invite othersInvite = invite(other, "anna@example.com");
            Invite someoneElse = invite(marek, "julia@example.com");

            service.delete(anna.getId(), PASSWORD);

            assertThat(accounts.findById(anna.getId())).isEmpty();
            assertThat(invites.findById(own.getId())).isEmpty();
            assertThat(invites.findById(othersInvite.getId())).isPresent();
            assertThat(invites.findById(someoneElse.getId())).isPresent();
            assertThat(accounts.findById(marek.getId())).isPresent();
            assertThat(sessions.ended).containsExactly(anna.getId());
        }

        @Test
        void shouldTellTheTrainerAClientLeft_withNothingOfTheClient() {
            Account marek = trainer("marek@example.com");
            Account anna = client("anna@example.com", marek);

            service.delete(anna.getId(), PASSWORD);

            assertThat(notices).containsExactly(new Notice(marek.getId().value(), null, NoticeKind.CLIENT_DELETED));
        }

        @Test
        void shouldTellNobody_whenATrainerOrAClientWithoutATrainerIsDeleted_orTheDeletionIsRefused() {
            Account marek = trainer("marek@example.com");
            Account alone = account("Alone", "alone@example.com", Role.CLIENT, null);
            Account anna = client("anna@example.com", marek);

            service.delete(alone.getId(), PASSWORD);
            assertThatThrownBy(() -> service.delete(anna.getId(), "wrong"))
                    .isInstanceOf(PasswordIncorrectException.class);

            assertThat(notices).isEmpty();
        }

        @Test
        void shouldDeleteATrainer_whenNoClientIsLeft() {
            Account marek = trainer("marek@example.com");

            service.delete(marek.getId(), PASSWORD);

            assertThat(accounts.findById(marek.getId())).isEmpty();
            assertThat(sessions.ended).containsExactly(marek.getId());
        }

        @Test
        void shouldRefuseATrainerWithClients_andChangeNothing() {
            Account marek = trainer("marek@example.com");
            client("anna@example.com", marek);
            client("ola@example.com", marek);

            assertThatThrownBy(() -> service.delete(marek.getId(), PASSWORD))
                    .isInstanceOfSatisfying(TrainerHasClientsException.class, refused -> {
                        assertThat(refused.code()).isEqualTo("account.has-clients");
                        assertThat(refused.properties()).containsEntry("clients", 2);
                    });
            assertThat(accounts.findById(marek.getId())).isPresent();
            assertThat(sessions.ended).isEmpty();
        }

        @Test
        void shouldRefuseAWrongPassword_deleteNothingAndCountAFailedSignIn() {
            Account anna = client("anna@example.com", trainer("marek@example.com"));

            assertThatThrownBy(() -> service.delete(anna.getId(), "Wrong-2026"))
                    .isInstanceOf(PasswordIncorrectException.class);

            assertThat(accounts.findById(anna.getId())).isPresent();
            assertThat(sessions.ended).isEmpty();
            assertThat(failedSignIns.find(anna.getEmail())).isPresent();
        }

        @Test
        void shouldRefuseEvenTheRightPassword_whenFiveWrongOnesLockedTheEmail() {
            Account anna = client("anna@example.com", trainer("marek@example.com"));
            for (int attempt = 0; attempt < 5; attempt++) {
                assertThatThrownBy(() -> service.delete(anna.getId(), "Wrong-2026"))
                        .isInstanceOf(PasswordIncorrectException.class);
            }

            assertThatThrownBy(() -> service.delete(anna.getId(), PASSWORD))
                    .isInstanceOfSatisfying(SignInLockedException.class, locked ->
                            assertThat(locked.code()).isEqualTo("account.sign-in-locked"));
            assertThat(accounts.findById(anna.getId())).isPresent();
        }

        @Test
        void shouldClearEarlierFailures_whenThePasswordIsRight() {
            Account marek = trainer("marek@example.com");
            assertThatThrownBy(() -> service.delete(marek.getId(), "Wrong-2026"))
                    .isInstanceOf(PasswordIncorrectException.class);

            service.delete(marek.getId(), PASSWORD);

            assertThat(failedSignIns.find(marek.getEmail())).isEmpty();
        }

        @Test
        void shouldRefuseAnOverlongPasswordWithoutHashingIt() {
            Account marek = trainer("marek@example.com");

            assertThatThrownBy(() -> service.delete(marek.getId(), "A-1" + "x".repeat(200)))
                    .isInstanceOf(PasswordIncorrectException.class);
            assertThat(hasher.comparedHashes).isEmpty();
        }
    }
}
