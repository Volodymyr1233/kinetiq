package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.ConfirmationLinkInvalidException;
import com.kinetiq.account.domain.input.SignUpTrainerCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.ConfirmationToken;
import com.kinetiq.account.domain.model.EmailConfirmation;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.output.Mail;
import com.kinetiq.common.domain.output.MailSender;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SignUpServiceTest {

    private static final Email EMAIL = new Email("marek@example.com");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final Locale POLISH = Locale.forLanguageTag("pl");
    private static final String APP_URL = "http://app.test";
    private static final String TOKEN_PREFIX = APP_URL + "/confirm-email#token=";

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final RecordingPasswordHasher hasher = new RecordingPasswordHasher();
    private final RecordingMailSender mails = new RecordingMailSender();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-19T10:00:00Z"));
    private final SignUpService service =
            new SignUpService(accounts, hasher, mails, clock, URI.create(APP_URL));

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
                assertThat(new ConfirmationToken(tokenOf(mail)).hash())
                        .isEqualTo(account.getConfirmation().orElseThrow().tokenHash());
            });
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
                    .isEqualTo(new ConfirmationToken(tokenOf(mails.sent.getLast())).hash());
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
            assertThatThrownBy(() -> service.confirmEmail(ConfirmationToken.generate().value()))
                    .isInstanceOf(ConfirmationLinkInvalidException.class);
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
                    .isEqualTo(new ConfirmationToken(tokenOf(mails.sent.getLast())).hash());
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
                AccountId.random(), "Marek Nowak", new Email(email), "hash", Role.TRAINER, WARSAW, true, null, null);
    }

    static class InMemoryAccountRepository implements AccountRepository {

        final Map<Email, Account> byEmail = new HashMap<>();

        Account add(Account account) {
            byEmail.put(account.getEmail(), account);
            return account;
        }

        @Override
        public void save(Account account) {
            add(account);
        }

        @Override
        public Optional<Account> findByEmail(Email email) {
            return Optional.ofNullable(byEmail.get(email));
        }

        @Override
        public Optional<Account> findByConfirmationTokenHash(String tokenHash) {
            return byEmail.values().stream()
                    .filter(account -> account.getConfirmation()
                            .map(EmailConfirmation::tokenHash)
                            .filter(tokenHash::equals)
                            .isPresent())
                    .findFirst();
        }
    }

    static class RecordingPasswordHasher implements PasswordHasher {

        int calls;

        @Override
        public String hash(RawPassword password) {
            calls++;
            return "hashed:" + password.value();
        }
    }

    static class RecordingMailSender implements MailSender {

        final List<Mail> sent = new ArrayList<>();

        @Override
        public void send(Mail mail) {
            sent.add(mail);
        }
    }

    static class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
