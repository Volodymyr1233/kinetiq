package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.CredentialsInvalidException;
import com.kinetiq.account.domain.exception.EmailNotConfirmedException;
import com.kinetiq.account.domain.exception.SignInLockedException;
import com.kinetiq.account.domain.input.SignInCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SignInServiceTest {

    private static final String PASSWORD = "Kinetiq-2026";

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final RecordingPasswordHasher hasher = new RecordingPasswordHasher();
    private final FakeFailedSignInsRepository failedSignIns = new FakeFailedSignInsRepository();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-24T10:00:00Z"));
    private final SignInService service = new SignInService(accounts, hasher, failedSignIns, clock);

    private static Account account(String email, Role role, boolean confirmed) {
        return Account.restore(
                AccountId.random(),
                "Marek Nowak",
                new Email(email),
                "hashed:" + PASSWORD,
                role,
                role == Role.CLIENT ? AccountId.random() : null,
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.EN),
                confirmed,
                null,
                null, null);
    }

    private Account signIn(String email, String password) {
        return service.signIn(new SignInCommand(email, password));
    }

    @Test
    void shouldReturnAccount_whenPasswordIsRightInAnyEmailCase() {
        Account trainer = accounts.add(account("marek@example.com", Role.TRAINER, true));

        assertThat(signIn(" Marek@Example.com ", PASSWORD)).isSameAs(trainer);
    }

    @Test
    void shouldRefuseAfterComparingDummyHash_whenEmailHasNoAccount() {
        hasher.comparedHashes.clear();

        assertThatThrownBy(() -> signIn("nobody@example.com", PASSWORD))
                .isInstanceOf(CredentialsInvalidException.class);
        assertThat(hasher.comparedHashes).singleElement().asString().startsWith("hashed:");
    }

    @Test
    void shouldRefuseTheSameWay_whenPasswordIsWrong() {
        accounts.add(account("marek@example.com", Role.TRAINER, true));

        assertThatThrownBy(() -> signIn("marek@example.com", "Kinetiq-2027"))
                .isInstanceOf(CredentialsInvalidException.class);
        assertThat(hasher.comparedHashes).hasSize(1);
    }

    @Test
    void shouldNotTrimPassword_whenItHasSurroundingSpaces() {
        accounts.add(account("marek@example.com", Role.TRAINER, true));

        assertThatThrownBy(() -> signIn("marek@example.com", " " + PASSWORD))
                .isInstanceOf(CredentialsInvalidException.class);
    }

    @Test
    void shouldRefuseWithoutHashing_whenEmailIsMalformed() {
        assertThatThrownBy(() -> signIn("not-an-email", PASSWORD)).isInstanceOf(CredentialsInvalidException.class);
        assertThat(hasher.comparedHashes).isEmpty();
    }

    @Test
    void shouldRefuseWithoutHashing_whenPasswordIsLongerThanAnyAccountCanHave() {
        accounts.add(account("marek@example.com", Role.TRAINER, true));

        assertThatThrownBy(() -> signIn("marek@example.com", "a".repeat(129)))
                .isInstanceOf(CredentialsInvalidException.class);
        assertThat(hasher.comparedHashes).isEmpty();
    }

    @Test
    void shouldRefuseUnconfirmedTrainer_onlyWhenPasswordIsRight() {
        accounts.add(account("marek@example.com", Role.TRAINER, false));

        assertThatThrownBy(() -> signIn("marek@example.com", PASSWORD))
                .isInstanceOf(EmailNotConfirmedException.class);
        assertThatThrownBy(() -> signIn("marek@example.com", "Kinetiq-2027"))
                .isInstanceOf(CredentialsInvalidException.class);
    }

    @Test
    void shouldSignInUnconfirmedClient_whenPasswordIsRight() {
        Account client = accounts.add(account("anna@example.com", Role.CLIENT, false));

        assertThat(signIn("anna@example.com", PASSWORD)).isSameAs(client);
    }

    @Nested
    class RepeatedFailures {

        private void failTimes(String email, int times) {
            for (int i = 0; i < times; i++) {
                assertThatThrownBy(() -> signIn(email, "Kinetiq-2027")).isInstanceOf(CredentialsInvalidException.class);
                clock.advance(Duration.ofSeconds(10));
            }
        }

        @Test
        void shouldAnswerInvalid_onTheFifthFailure() {
            accounts.add(account("marek@example.com", Role.TRAINER, true));

            failTimes("marek@example.com", 5);
        }

        @Test
        void shouldRefuseRightPasswordWithoutHashing_whenEmailIsLocked() {
            accounts.add(account("marek@example.com", Role.TRAINER, true));
            failTimes("marek@example.com", 5);
            hasher.comparedHashes.clear();

            assertThatThrownBy(() -> signIn("Marek@Example.com", PASSWORD))
                    .isInstanceOfSatisfying(SignInLockedException.class, locked -> assertThat(
                                    locked.properties().get("retryAfterSeconds"))
                            .isEqualTo(15L * 60 - 10));
            assertThat(hasher.comparedHashes).isEmpty();
        }

        @Test
        void shouldLockTheSameWay_whenEmailHasNoAccount() {
            failTimes("nobody@example.com", 5);

            assertThatThrownBy(() -> signIn("nobody@example.com", PASSWORD))
                    .isInstanceOf(SignInLockedException.class);
        }

        @Test
        void shouldSignInAgain_whenLockHasEnded() {
            Account trainer = accounts.add(account("marek@example.com", Role.TRAINER, true));
            failTimes("marek@example.com", 5);

            clock.advance(Duration.ofMinutes(15));

            assertThat(signIn("marek@example.com", PASSWORD)).isSameAs(trainer);
        }

        @Test
        void shouldCountFromZero_whenRightPasswordWasGiven() {
            Account trainer = accounts.add(account("marek@example.com", Role.TRAINER, true));
            failTimes("marek@example.com", 4);

            signIn("marek@example.com", PASSWORD);
            failTimes("marek@example.com", 4);

            assertThat(signIn("marek@example.com", PASSWORD)).isSameAs(trainer);
        }

        @Test
        void shouldCountFromZero_whenUnconfirmedTrainerGaveRightPassword() {
            accounts.add(account("marek@example.com", Role.TRAINER, false));
            failTimes("marek@example.com", 4);

            assertThatThrownBy(() -> signIn("marek@example.com", PASSWORD))
                    .isInstanceOf(EmailNotConfirmedException.class);
            failTimes("marek@example.com", 4);

            assertThatThrownBy(() -> signIn("marek@example.com", PASSWORD))
                    .isInstanceOf(EmailNotConfirmedException.class);
        }
    }
}
