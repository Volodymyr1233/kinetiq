package com.kinetiq.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LanguageTest {

    @ParameterizedTest
    @ValueSource(strings = {"pl", "pl-PL"})
    void shouldBePolish_whenLocaleIsPolish(String tag) {
        assertThat(Language.from(Locale.forLanguageTag(tag))).isEqualTo(Language.PL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "en-GB", "de", ""})
    void shouldBeEnglish_whenLocaleIsAnyOther(String tag) {
        assertThat(Language.from(Locale.forLanguageTag(tag))).isEqualTo(Language.EN);
    }
}
