package com.kinetiq.exercise.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocalizedTextTest {

    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    @Test
    void shouldReturnPolish_whenPolishIsAskedAndPresent() {
        assertThat(new LocalizedText("Plank", "Deska").in(POLISH)).isEqualTo("Deska");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void shouldFallBackToEnglish_whenPolishIsAskedButEmpty(String polish) {
        assertThat(new LocalizedText("Plank", polish).in(POLISH)).isEqualTo("Plank");
    }

    @Test
    void shouldFallBackToEnglish_whenPolishIsMissing() {
        assertThat(new LocalizedText("Plank", null).in(POLISH)).isEqualTo("Plank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "de", "uk"})
    void shouldReturnEnglish_whenAnyOtherLanguageIsAsked(String language) {
        assertThat(new LocalizedText("Plank", "Deska").in(Locale.forLanguageTag(language)))
                .isEqualTo("Plank");
    }
}
