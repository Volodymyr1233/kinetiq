package com.kinetiq.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.common.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RawPasswordTest {

    private static String ofLength(int length) {
        return "Aa1!" + "a".repeat(length - 4);
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 128})
    void shouldAccept_whenLengthIsAtTheLimit(int length) {
        assertThat(new RawPassword(ofLength(length)).value()).hasSize(length);
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 129})
    void shouldReject_whenLengthIsJustOutside(int length) {
        assertThatThrownBy(() -> new RawPassword(ofLength(length))).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"kinetiq-2026", "KINETIQ-2026", "Kinetiq-dwa", "Kinetiq2026"})
    void shouldReject_whenACharacterClassIsMissing(String value) {
        assertThatThrownBy(() -> new RawPassword(value)).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Kinetiq 2026", "Kinetiqą2026"})
    void shouldCountSpaceAndNonLatinLetter_asOtherCharacter(String value) {
        assertThat(new RawPassword(value).value()).isEqualTo(value);
    }

    @Test
    void shouldHideValue_whenPrinted() {
        assertThat(new RawPassword("Kinetiq-2026")).hasToString("***");
    }
}
