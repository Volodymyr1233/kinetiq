package com.kinetiq.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AccountTest {

    private static final Email EMAIL = new Email("marek@example.com");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    static Account trainer() {
        return Account.signUpTrainer("Marek Nowak", EMAIL, "hash", WARSAW);
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
        void shouldTrimName_whenSurroundedBySpaces() {
            assertThat(Account.signUpTrainer("  Marek Nowak ", EMAIL, "hash", WARSAW).getName())
                    .isEqualTo("Marek Nowak");
        }

        @Test
        void shouldAcceptName_whenItHasPunctuationAndOtherAlphabets() {
            assertThat(Account.signUpTrainer("Zoë O'Brien-Łukasik", EMAIL, "hash", WARSAW).getName())
                    .isEqualTo("Zoë O'Brien-Łukasik");
        }

        @Test
        void shouldAcceptName_whenExactly100Characters() {
            assertThat(Account.signUpTrainer("a".repeat(100), EMAIL, "hash", WARSAW).getName())
                    .hasSize(100);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   ", "Marek\nNowak", "Marek\tNowak"})
        void shouldRejectName_whenBlankOrHasControlCharacters(String name) {
            assertThatThrownBy(() -> Account.signUpTrainer(name, EMAIL, "hash", WARSAW))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRejectName_when101Characters() {
            assertThatThrownBy(() -> Account.signUpTrainer("a".repeat(101), EMAIL, "hash", WARSAW))
                    .isInstanceOf(InvalidValueException.class);
        }
    }
}
