package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.PasswordResetLinkInvalidException;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.output.Mail;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PasswordResetServiceTest {

    private static final Email EMAIL = new Email("marek@example.com");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final Locale POLISH = Locale.forLanguageTag("pl");
    private static final String APP_URL = "http://app.test";
    private static final String TOKEN_PREFIX = APP_URL + "/reset-password#token=";

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final RecordingPasswordHasher hasher = new RecordingPasswordHasher();
    private final RecordingAccountSessions sessions = new RecordingAccountSessions();
    private final RecordingMailSender mails = new RecordingMailSender();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-25T10:00:00Z"));
    private final PasswordResetService service =
            new PasswordResetService(accounts, hasher, sessions, mails, clock, URI.create(APP_URL));

    private Account confirmedAccount() {
        return accounts.add(Account.restore(
                AccountId.random(),
                "Marek Nowak",
                EMAIL,
                "old-hash",
                Role.TRAINER,
                WARSAW,
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
                null));
    }

    private static String tokenOf(Mail mail) {
        return mail.values().get("link").substring(TOKEN_PREFIX.length());
    }

    @Nested
    class RequestReset {

        @Test
        void shouldSendLinkInGivenLanguage_whenAccountIsConfirmed() {
            Account account = confirmedAccount();

            service.requestReset(new Email("Marek@Example.com"), POLISH);

            assertThat(mails.sent).singleElement().satisfies(mail -> {
                assertThat(mail.to()).isEqualTo(EMAIL);
                assertThat(mail.template()).isEqualTo("reset-password");
                assertThat(mail.locale()).isEqualTo(POLISH);
                assertThat(new SecretToken(tokenOf(mail)).hash())
                        .isEqualTo(account.getPasswordReset().orElseThrow().tokenHash());
            });
        }

        @Test
        void shouldSendNothing_whenEmailHasNoAccount() {
            service.requestReset(EMAIL, POLISH);

            assertThat(mails.sent).isEmpty();
            assertThat(accounts.saves).isZero();
        }

        @Test
        void shouldSendNothing_whenEmailIsNotConfirmed() {
            accounts.add(Account.signUpTrainer("Marek Nowak", EMAIL, "hash", WARSAW, Language.EN));

            service.requestReset(EMAIL, POLISH);

            assertThat(mails.sent).isEmpty();
        }

        @Test
        void shouldSendNothing_whenAMailWentOutLessThan60SecondsAgo() {
            confirmedAccount();
            service.requestReset(EMAIL, POLISH);
            clock.advance(Duration.ofSeconds(59));

            service.requestReset(EMAIL, POLISH);

            assertThat(mails.sent).hasSize(1);
        }

        @Test
        void shouldReplaceEarlierLink_whenAskedAgainLater() {
            confirmedAccount();
            service.requestReset(EMAIL, POLISH);
            String first = tokenOf(mails.sent.getFirst());
            clock.advance(Duration.ofMinutes(5));

            service.requestReset(EMAIL, POLISH);

            String second = tokenOf(mails.sent.getLast());
            assertThatThrownBy(() -> service.setPassword(first, new RawPassword("Kinetiq-2026")))
                    .isInstanceOf(PasswordResetLinkInvalidException.class);
            service.setPassword(second, new RawPassword("Kinetiq-2026"));
        }
    }

    @Nested
    class SetPassword {

        @Test
        void shouldStoreNewHashAndEndEverySession_whenLinkIsValid() {
            Account account = confirmedAccount();
            service.requestReset(EMAIL, POLISH);

            service.setPassword(tokenOf(mails.sent.getFirst()), new RawPassword("New-pass-2026"));

            assertThat(account.getPasswordHash()).isEqualTo("hashed:New-pass-2026");
            assertThat(sessions.ended).containsExactly(account.getId());
        }

        @Test
        void shouldRefuseSecondUse_whenLinkWasUsed() {
            confirmedAccount();
            service.requestReset(EMAIL, POLISH);
            String token = tokenOf(mails.sent.getFirst());
            service.setPassword(token, new RawPassword("New-pass-2026"));

            assertThatThrownBy(() -> service.setPassword(token, new RawPassword("Other-pass-2026")))
                    .isInstanceOf(PasswordResetLinkInvalidException.class);
            assertThat(sessions.ended).hasSize(1);
        }

        @Test
        void shouldRefuseAndKeepPassword_whenLinkIsUnknown() {
            Account account = confirmedAccount();

            assertThatThrownBy(() -> service.setPassword("unknown", new RawPassword("New-pass-2026")))
                    .isInstanceOf(PasswordResetLinkInvalidException.class);
            assertThat(account.getPasswordHash()).isEqualTo("old-hash");
            assertThat(sessions.ended).isEmpty();
        }

        @Test
        void shouldRefuse_whenAnHourPassed() {
            Account account = confirmedAccount();
            service.requestReset(EMAIL, POLISH);
            clock.advance(Duration.ofHours(1));

            assertThatThrownBy(
                            () -> service.setPassword(tokenOf(mails.sent.getFirst()), new RawPassword("New-pass-2026")))
                    .isInstanceOf(PasswordResetLinkInvalidException.class);
            assertThat(account.getPasswordHash()).isEqualTo("old-hash");
            assertThat(sessions.ended).isEmpty();
        }
    }
}
