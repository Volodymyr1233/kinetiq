package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.ConfirmationLinkInvalidException;
import com.kinetiq.account.domain.exception.InviteLinkInvalidException;
import com.kinetiq.account.domain.input.InviteDetails;
import com.kinetiq.account.domain.input.JoinCommand;
import com.kinetiq.account.domain.input.SignUpTrainerCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteStatus;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.model.SentInvite;
import com.kinetiq.account.domain.model.Theme;
import com.kinetiq.account.domain.model.WeightUnit;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.output.Mail;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SignUpServiceTest {

    private static final Email EMAIL = new Email("marek@example.com");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final Locale POLISH = Locale.forLanguageTag("pl");
    private static final String APP_URL = "http://app.test";
    private static final String TOKEN_PREFIX = APP_URL + "/confirm-email#token=";

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryInviteRepository invites = new InMemoryInviteRepository();
    private final RecordingPasswordHasher hasher = new RecordingPasswordHasher();
    private final RecordingMailSender mails = new RecordingMailSender();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-19T10:00:00Z"));
    private final SignUpService service =
            new SignUpService(accounts, invites, hasher, mails, clock, URI.create(APP_URL));

    private static SignUpTrainerCommand signUp(String email, String password) {
        return new SignUpTrainerCommand("Marek Nowak", new Email(email), new RawPassword(password), WARSAW, POLISH);
    }

    private static String tokenOf(Mail mail) {
        return mail.values().get("link").substring(TOKEN_PREFIX.length());
    }

    @Nested
    class SignUpTrainer {

        @Test
        void shouldCreateUnconfirmedTrainerAndSendLink_whenEmailIsNew() {
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));

            Account account = accounts.byEmail.get(EMAIL);
            assertThat(account.getRole()).isEqualTo(Role.TRAINER);
            assertThat(account.isEmailConfirmed()).isFalse();
            assertThat(account.getTimeZone()).isEqualTo(WARSAW);
            assertThat(account.getPasswordHash()).isEqualTo("hashed:Kinetiq-2026");
            assertThat(mails.sent).singleElement().satisfies(mail -> {
                assertThat(mail.to()).isEqualTo(EMAIL);
                assertThat(mail.template()).isEqualTo("confirm-email");
                assertThat(mail.locale()).isEqualTo(POLISH);
                assertThat(mail.values().get("link")).startsWith(TOKEN_PREFIX);
                assertThat(new SecretToken(tokenOf(mail)).hash())
                        .isEqualTo(account.getConfirmation().orElseThrow().tokenHash());
            });
        }

        @Test
        void shouldStoreUiLanguage_whenSignedUp() {
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            service.signUpTrainer(new SignUpTrainerCommand(
                    "Anna Nowak",
                    new Email("anna@example.com"),
                    new RawPassword("Kinetiq-2026"),
                    WARSAW,
                    Locale.ENGLISH));

            assertThat(accounts.byEmail.get(EMAIL).getSettings().language()).isEqualTo(Language.PL);
            assertThat(accounts.byEmail.get(new Email("anna@example.com")).getSettings().language())
                    .isEqualTo(Language.EN);
        }

        @Test
        void shouldSendNewLinkAndKeepAccount_whenEmailHasUnconfirmedAccount() {
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            Account account = accounts.byEmail.get(EMAIL);
            String firstHash = account.getConfirmation().orElseThrow().tokenHash();
            clock.advance(Duration.ofMinutes(5));

            service.signUpTrainer(signUp("Marek@Example.com", "Other-pass-1"));

            assertThat(accounts.byEmail).hasSize(1);
            assertThat(accounts.byEmail.get(EMAIL).getId()).isEqualTo(account.getId());
            assertThat(account.getPasswordHash()).isEqualTo("hashed:Kinetiq-2026");
            assertThat(account.getConfirmation().orElseThrow().tokenHash())
                    .isNotEqualTo(firstHash)
                    .isEqualTo(new SecretToken(tokenOf(mails.sent.getLast())).hash());
            assertThat(mails.sent).extracting(Mail::template).containsExactly("confirm-email", "confirm-email");
        }

        @Test
        void shouldSendAccountExistsMail_whenEmailHasConfirmedAccount() {
            Account confirmed = accounts.add(confirmedAccount());

            service.signUpTrainer(signUp("marek@example.com", "Other-pass-1"));

            assertThat(mails.sent).singleElement().satisfies(mail -> {
                assertThat(mail.template()).isEqualTo("account-exists");
                assertThat(mail.values()).containsEntry("link", APP_URL + "/sign-in");
            });
            assertThat(confirmed.getPasswordHash()).isEqualTo("hash");
            assertThat(confirmed.getConfirmation()).isEmpty();
            assertThat(confirmed.getMailSentAt()).contains(clock.instant());
        }

        @Test
        void shouldHashPassword_onEveryBranch() {
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            accounts.add(confirmedAccount("anna@example.com"));
            service.signUpTrainer(signUp("anna@example.com", "Kinetiq-2026"));

            assertThat(hasher.calls).isEqualTo(3);
        }

        @Test
        void shouldSendNothing_whenPreviousMailWasLessThan60SecondsAgo() {
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            String firstHash = accounts.byEmail.get(EMAIL).getConfirmation().orElseThrow().tokenHash();
            clock.advance(Duration.ofSeconds(59));

            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));

            assertThat(mails.sent).hasSize(1);
            assertThat(accounts.byEmail.get(EMAIL).getConfirmation().orElseThrow().tokenHash()).isEqualTo(firstHash);
        }

        @Test
        void shouldSendNothing_whenConfirmedAccountGotMailLessThan60SecondsAgo() {
            accounts.add(confirmedAccount());
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            clock.advance(Duration.ofSeconds(30));

            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));

            assertThat(mails.sent).hasSize(1);
        }
    }

    @Nested
    class ConfirmEmail {

        private String signUpAndGetToken() {
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            return tokenOf(mails.sent.getLast());
        }

        @Test
        void shouldConfirm_whenLinkIsValid() {
            String token = signUpAndGetToken();

            service.confirmEmail(token);

            assertThat(accounts.byEmail.get(EMAIL).isEmailConfirmed()).isTrue();
        }

        @Test
        void shouldRefuse_whenLinkWasUsed() {
            String token = signUpAndGetToken();
            service.confirmEmail(token);

            assertThatThrownBy(() -> service.confirmEmail(token)).isInstanceOf(ConfirmationLinkInvalidException.class);
        }

        @Test
        void shouldRefuse_whenLinkExpired() {
            String token = signUpAndGetToken();
            clock.advance(Duration.ofHours(24));

            assertThatThrownBy(() -> service.confirmEmail(token)).isInstanceOf(ConfirmationLinkInvalidException.class);
            assertThat(accounts.byEmail.get(EMAIL).isEmailConfirmed()).isFalse();
        }

        @Test
        void shouldRefuse_whenLinkWasReplaced() {
            String first = signUpAndGetToken();
            clock.advance(Duration.ofMinutes(1));
            service.sendConfirmationLink(EMAIL, POLISH);

            assertThatThrownBy(() -> service.confirmEmail(first)).isInstanceOf(ConfirmationLinkInvalidException.class);
            service.confirmEmail(tokenOf(mails.sent.getLast()));
            assertThat(accounts.byEmail.get(EMAIL).isEmailConfirmed()).isTrue();
        }

        @Test
        void shouldRefuse_whenLinkIsUnknown() {
            assertThatThrownBy(() -> service.confirmEmail(SecretToken.generate().value()))
                    .isInstanceOf(ConfirmationLinkInvalidException.class);
        }
    }

    @Nested
    class JoinThroughInvite {

        private static final Email INVITED = new Email("anna@example.com");

        private final Account trainer = accounts.add(confirmedAccount());
        private final SentInvite sent = send(trainer);
        private final Invite invite = sent.invite();

        private SentInvite send(Account from) {
            SentInvite created = Invite.send(from.getId(), INVITED, clock.instant());
            invites.save(created.invite());
            return created;
        }

        private JoinCommand join(SecretToken token) {
            return new JoinCommand(token.value(), "  Anna Kowalska ", new RawPassword("Kinetiq-2026"), WARSAW, POLISH);
        }

        private void assertRefusedChangingNothing(SecretToken token) {
            int saves = accounts.saves;
            int hashes = hasher.calls;
            InviteStatus status = invite.getStatus();

            assertThatThrownBy(() -> service.checkInvite(token.value())).isInstanceOf(InviteLinkInvalidException.class);
            assertThatThrownBy(() -> service.joinAsClient(join(token))).isInstanceOf(InviteLinkInvalidException.class);
            assertThat(accounts.saves).isEqualTo(saves);
            assertThat(accounts.byEmail).doesNotContainKey(INVITED);
            assertThat(hasher.calls).isEqualTo(hashes);
            assertThat(invite.getStatus()).isEqualTo(status);
        }

        @Test
        void shouldTellEmailAndTrainerAndChangeNothing_whenEitherSecretIsChecked() {
            for (SecretToken token : new SecretToken[] {sent.emailToken(), sent.linkToken()}) {
                assertThat(service.checkInvite(token.value())).isEqualTo(new InviteDetails(INVITED, "Marek Nowak"));
            }
            assertThat(invite.getStatus()).isEqualTo(InviteStatus.OPEN);
            assertThat(accounts.saves).isZero();
            assertThat(hasher.calls).isZero();
        }

        @Test
        void shouldCreateConfirmedClientOfTrainerAndUseInvite_whenEmailedSecretIsUsed() {
            Account client = service.joinAsClient(join(sent.emailToken()));

            assertThat(accounts.byEmail.get(INVITED)).isSameAs(client);
            assertThat(client.getRole()).isEqualTo(Role.CLIENT);
            assertThat(client.getName()).isEqualTo("Anna Kowalska");
            assertThat(client.getPasswordHash()).isEqualTo("hashed:Kinetiq-2026");
            assertThat(client.getTimeZone()).isEqualTo(WARSAW);
            assertThat(client.isEmailConfirmed()).isTrue();
            assertThat(client.getTrainerId()).contains(trainer.getId());
            assertThat(client.getHealthDataConsentAt()).contains(clock.instant());
            assertThat(client.getSettings()).isEqualTo(new AccountSettings(WeightUnit.KG, Language.PL, Theme.SYSTEM));
            assertThat(invite.getStatus()).isEqualTo(InviteStatus.USED);
            assertThat(accounts.findClientsOf(trainer.getId())).containsExactly(client);
        }

        @Test
        void shouldStartWithTrainersUnitsAndUiLanguage_whenTrainerUsesPounds() {
            trainer.changeSettings(new AccountSettings(WeightUnit.LB, Language.PL, Theme.DARK));

            Account client = service.joinAsClient(new JoinCommand(
                    sent.emailToken().value(),
                    "Anna Kowalska",
                    new RawPassword("Kinetiq-2026"),
                    WARSAW,
                    Locale.ENGLISH));

            assertThat(client.getSettings()).isEqualTo(new AccountSettings(WeightUnit.LB, Language.EN, Theme.SYSTEM));
        }

        @Test
        void shouldCreateUnconfirmedClient_whenCopiedSecretIsUsed() {
            Account client = service.joinAsClient(join(sent.linkToken()));

            assertThat(client.isEmailConfirmed()).isFalse();
            assertThat(client.getTrainerId()).contains(trainer.getId());
            assertThat(invite.getStatus()).isEqualTo(InviteStatus.USED);
        }

        @Test
        void shouldMailConfirmationLinkThatConfirms_whenCopiedSecretIsUsed() {
            Account client = service.joinAsClient(join(sent.linkToken()));

            assertThat(mails.sent).singleElement().satisfies(mail -> {
                assertThat(mail.to()).isEqualTo(INVITED);
                assertThat(mail.template()).isEqualTo("confirm-email");
                assertThat(mail.locale()).isEqualTo(POLISH);
            });
            service.confirmEmail(tokenOf(mails.sent.getFirst()));
            assertThat(client.isEmailConfirmed()).isTrue();
        }

        @Test
        void shouldMailNothing_whenEmailedSecretIsUsed() {
            service.joinAsClient(join(sent.emailToken()));

            assertThat(mails.sent).isEmpty();
        }

        @Test
        void shouldRefuseSecondJoin_whenInviteWasUsed() {
            service.joinAsClient(join(sent.emailToken()));
            // without the account, only the used invite stands in the way
            accounts.byEmail.remove(INVITED);

            assertRefusedChangingNothing(sent.emailToken());
            assertRefusedChangingNothing(sent.linkToken());
        }

        @Test
        void shouldRefuse_whenInviteExpiredExactlySevenDaysAfterSending() {
            clock.advance(Invite.VALIDITY);

            assertRefusedChangingNothing(sent.emailToken());
        }

        @Test
        void shouldRefuse_whenInviteWasRevoked() {
            invite.revoke(clock.instant());

            assertRefusedChangingNothing(sent.emailToken());
        }

        @Test
        void shouldRefuse_whenInviteWasSuperseded() {
            invite.supersede();

            assertRefusedChangingNothing(sent.linkToken());
        }

        @Test
        void shouldRefuseOldCopiedLinkOnly_whenCopiedLinkWasReplaced() {
            SecretToken newLink = invite.replaceLink(clock.instant());

            assertRefusedChangingNothing(sent.linkToken());
            assertThat(service.checkInvite(newLink.value()).email()).isEqualTo(INVITED);
        }

        @Test
        void shouldRefuse_whenTokenIsUnknown() {
            assertRefusedChangingNothing(SecretToken.generate());
        }

        @Test
        void shouldRefuse_whenInvitedEmailGotAnAccount() {
            accounts.add(confirmedAccount(INVITED.value()));
            int saves = accounts.saves;

            assertThatThrownBy(() -> service.checkInvite(sent.emailToken().value()))
                    .isInstanceOf(InviteLinkInvalidException.class);
            assertThatThrownBy(() -> service.joinAsClient(join(sent.emailToken())))
                    .isInstanceOf(InviteLinkInvalidException.class);
            assertThat(accounts.saves).isEqualTo(saves);
            assertThat(hasher.calls).isZero();
            assertThat(invite.getStatus()).isEqualTo(InviteStatus.OPEN);
        }

        @Test
        void shouldMakeClientOfJoinedTrainerOnly_whenTwoTrainersInvitedTheEmail() {
            Account ola = accounts.add(confirmedAccount("ola@example.com"));
            SentInvite fromOla = send(ola);

            Account client = service.joinAsClient(join(fromOla.linkToken()));

            assertThat(client.getTrainerId()).contains(ola.getId());
            assertThat(accounts.findClientsOf(ola.getId())).containsExactly(client);
            assertThat(accounts.findClientsOf(trainer.getId())).isEmpty();
            assertThatThrownBy(() -> service.checkInvite(sent.emailToken().value()))
                    .isInstanceOf(InviteLinkInvalidException.class);
        }
    }

    @Nested
    class SendConfirmationLink {

        @Test
        void shouldSendNewLink_whenAccountIsUnconfirmed() {
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            clock.advance(Duration.ofSeconds(60));

            service.sendConfirmationLink(EMAIL, Locale.ENGLISH);

            assertThat(mails.sent).hasSize(2);
            assertThat(mails.sent.getLast().locale()).isEqualTo(Locale.ENGLISH);
            assertThat(accounts.byEmail.get(EMAIL).getConfirmation().orElseThrow().tokenHash())
                    .isEqualTo(new SecretToken(tokenOf(mails.sent.getLast())).hash());
        }

        @Test
        void shouldSendNothing_whenAccountIsConfirmed() {
            accounts.add(confirmedAccount());

            service.sendConfirmationLink(EMAIL, POLISH);

            assertThat(mails.sent).isEmpty();
        }

        @Test
        void shouldSendNothing_whenEmailHasNoAccount() {
            service.sendConfirmationLink(EMAIL, POLISH);

            assertThat(mails.sent).isEmpty();
        }

        @Test
        void shouldSendNothing_whenPreviousMailWasLessThan60SecondsAgo() {
            service.signUpTrainer(signUp("marek@example.com", "Kinetiq-2026"));
            clock.advance(Duration.ofSeconds(59));

            service.sendConfirmationLink(EMAIL, POLISH);

            assertThat(mails.sent).hasSize(1);
        }
    }

    static Account confirmedAccount() {
        return confirmedAccount("marek@example.com");
    }

    static Account confirmedAccount(String email) {
        return Account.restore(
                AccountId.random(),
                "Marek Nowak",
                new Email(email),
                "hash",
                Role.TRAINER,
                null,
                WARSAW,
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
                null,
                null);
    }
}
