package com.kinetiq.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.ConfirmationLinkInvalidException;
import com.kinetiq.account.domain.exception.PasswordResetLinkInvalidException;
import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AccountTest {

    private static final Email EMAIL = new Email("marek@example.com");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");
    private static final Duration DAY = Duration.ofHours(24);

    static Account trainer() {
        return Account.signUpTrainer("Marek Nowak", EMAIL, "hash", WARSAW, Language.EN);
    }

    @Nested
    class SignUpTrainer {

        @Test
        void shouldCreateUnconfirmedTrainer_whenSignedUp() {
            Account account = trainer();

            assertThat(account.getRole()).isEqualTo(Role.TRAINER);
            assertThat(account.isEmailConfirmed()).isFalse();
        }

        @Test
        void shouldStartWithKilogramsSignUpLanguageAndDeviceTheme_whenSignedUp() {
            Account account = Account.signUpTrainer("Marek Nowak", EMAIL, "hash", WARSAW, Language.PL);

            assertThat(account.getSettings()).isEqualTo(new AccountSettings(WeightUnit.KG, Language.PL, Theme.SYSTEM));
        }

        @Test
        void shouldTrimName_whenSurroundedBySpaces() {
            assertThat(Account.signUpTrainer("  Marek Nowak ", EMAIL, "hash", WARSAW, Language.EN).getName())
                    .isEqualTo("Marek Nowak");
        }

        @Test
        void shouldAcceptName_whenItHasPunctuationAndOtherAlphabets() {
            assertThat(Account.signUpTrainer("Zoë O'Brien-Łukasik", EMAIL, "hash", WARSAW, Language.EN).getName())
                    .isEqualTo("Zoë O'Brien-Łukasik");
        }

        @Test
        void shouldAcceptName_whenExactly100Characters() {
            assertThat(Account.signUpTrainer("a".repeat(100), EMAIL, "hash", WARSAW, Language.EN).getName())
                    .hasSize(100);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   ", "Marek\nNowak", "Marek\tNowak"})
        void shouldRejectName_whenBlankOrHasControlCharacters(String name) {
            assertThatThrownBy(() -> Account.signUpTrainer(name, EMAIL, "hash", WARSAW, Language.EN))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRejectName_when101Characters() {
            assertThatThrownBy(() -> Account.signUpTrainer("a".repeat(101), EMAIL, "hash", WARSAW, Language.EN))
                    .isInstanceOf(InvalidValueException.class);
        }
    }

    @Nested
    class ChangeSettings {

        @Test
        void shouldReplaceSettings_whenChanged() {
            Account account = trainer();
            AccountSettings chosen = new AccountSettings(WeightUnit.LB, Language.PL, Theme.DARK);

            account.changeSettings(chosen);

            assertThat(account.getSettings()).isEqualTo(chosen);
        }

        @Test
        void shouldRefuse_whenSettingsAreMissing() {
            assertThatThrownBy(() -> trainer().changeSettings(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class IssueConfirmation {

        @Test
        void shouldExpireIn24Hours_whenIssued() {
            Account account = trainer();
            SecretToken token = SecretToken.generate();

            account.issueConfirmation(token, NOW);

            assertThat(account.getConfirmation()).contains(new EmailConfirmation(token.hash(), NOW.plus(DAY)));
            assertThat(account.getMailSentAt()).contains(NOW);
        }

        @Test
        void shouldReplaceEarlierLink_whenIssuedAgain() {
            Account account = trainer();
            account.issueConfirmation(SecretToken.generate(), NOW);
            SecretToken newer = SecretToken.generate();

            account.issueConfirmation(newer, NOW.plusSeconds(60));

            assertThat(account.getConfirmation().orElseThrow().tokenHash()).isEqualTo(newer.hash());
        }

        @Test
        void shouldRefuse_whenEmailIsConfirmed() {
            Account account = confirmed();

            assertThatThrownBy(() -> account.issueConfirmation(SecretToken.generate(), NOW))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class CanReceiveMail {

        @Test
        void shouldAllow_whenNoMailWasSent() {
            assertThat(trainer().canReceiveMail(NOW)).isTrue();
        }

        @Test
        void shouldRefuse_when59SecondsPassed() {
            Account account = trainer();
            account.recordMailSent(NOW);

            assertThat(account.canReceiveMail(NOW.plusSeconds(59))).isFalse();
        }

        @Test
        void shouldAllow_when60SecondsPassed() {
            Account account = trainer();
            account.recordMailSent(NOW);

            assertThat(account.canReceiveMail(NOW.plusSeconds(60))).isTrue();
        }
    }

    @Nested
    class ConfirmEmail {

        @Test
        void shouldConfirmAndRemoveLink_whenLinkIsValid() {
            Account account = trainer();
            account.issueConfirmation(SecretToken.generate(), NOW);

            account.confirmEmail(NOW.plus(DAY).minusSeconds(1));

            assertThat(account.isEmailConfirmed()).isTrue();
            assertThat(account.getConfirmation()).isEmpty();
        }

        @Test
        void shouldRefuse_whenConfirmedTwice() {
            Account account = trainer();
            account.issueConfirmation(SecretToken.generate(), NOW);
            account.confirmEmail(NOW);

            assertThatThrownBy(() -> account.confirmEmail(NOW)).isInstanceOf(ConfirmationLinkInvalidException.class);
        }

        @Test
        void shouldRefuseAndStayUnconfirmed_whenExactly24HoursPassed() {
            Account account = trainer();
            account.issueConfirmation(SecretToken.generate(), NOW);

            assertThatThrownBy(() -> account.confirmEmail(NOW.plus(DAY)))
                    .isInstanceOf(ConfirmationLinkInvalidException.class);
            assertThat(account.isEmailConfirmed()).isFalse();
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void shouldExpireInAnHour_whenIssued() {
            Account account = confirmed();
            SecretToken token = SecretToken.generate();

            account.issuePasswordReset(token, NOW);

            assertThat(account.getPasswordReset())
                    .contains(new PasswordReset(token.hash(), NOW.plus(Duration.ofHours(1))));
            assertThat(account.getMailSentAt()).contains(NOW);
        }

        @Test
        void shouldChangePasswordAndRemoveLink_whenLinkIsValid() {
            Account account = confirmed();
            account.issuePasswordReset(SecretToken.generate(), NOW);

            account.resetPassword("new-hash", NOW.plusSeconds(59 * 60));

            assertThat(account.getPasswordHash()).isEqualTo("new-hash");
            assertThat(account.getPasswordReset()).isEmpty();
        }

        @Test
        void shouldRefuseAndKeepPassword_whenExactlyAnHourPassed() {
            Account account = confirmed();
            account.issuePasswordReset(SecretToken.generate(), NOW);

            assertThatThrownBy(() -> account.resetPassword("new-hash", NOW.plus(Duration.ofHours(1))))
                    .isInstanceOf(PasswordResetLinkInvalidException.class);
            assertThat(account.getPasswordHash()).isEqualTo("hash");
        }

        @Test
        void shouldRefuse_whenNoLinkWasIssued() {
            assertThatThrownBy(() -> confirmed().resetPassword("new-hash", NOW))
                    .isInstanceOf(PasswordResetLinkInvalidException.class);
        }
    }

    static Account confirmed() {
        return Account.restore(
                AccountId.random(),
                "Marek Nowak",
                EMAIL,
                "hash",
                Role.TRAINER,
                WARSAW,
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
                null);
    }
}
