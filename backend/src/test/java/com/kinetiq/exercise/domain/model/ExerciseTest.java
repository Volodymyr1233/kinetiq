package com.kinetiq.exercise.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.common.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExerciseTest {

    private static final LocalizedText EQUIPMENT = new LocalizedText("Barbell", "Sztanga");
    private static final LocalizedText CUES = new LocalizedText("Brace first.", "Najpierw napnij brzuch.");

    static Exercise exercise(LocalizedText name, int sets, int amount, int rest) {
        return new Exercise(
                ExerciseId.random(), null, name, Category.LEGS, EQUIPMENT, Measure.REPS, sets, amount, rest, CUES);
    }

    static Exercise exercise(String english, String polish) {
        return exercise(new LocalizedText(english, polish), 3, 5, 120);
    }

    @Nested
    class Constructor {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void shouldRefuse_whenEnglishNameIsBlank(String name) {
            assertThatThrownBy(() -> exercise(name, "Przysiad")).isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRefuse_whenEnglishNameIsLongerThan100() {
            assertThatThrownBy(() -> exercise("a".repeat(101), "Przysiad"))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRefuse_whenPolishNameIsLongerThan100() {
            assertThatThrownBy(() -> exercise("Back squat", "a".repeat(101)))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRefuse_whenEquipmentIsLongerThan50() {
            assertThatThrownBy(() -> new Exercise(
                            ExerciseId.random(), null, new LocalizedText("Back squat", ""), Category.LEGS,
                            new LocalizedText("a".repeat(51), ""), Measure.REPS, 3, 5, 120, CUES))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRefuse_whenPolishCuesAreLongerThan500() {
            assertThatThrownBy(() -> new Exercise(
                            ExerciseId.random(), null, new LocalizedText("Back squat", ""), Category.LEGS,
                            EQUIPMENT, Measure.REPS, 3, 5, 120, new LocalizedText("", "a".repeat(501))))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRefuse_whenSetsAre0() {
            assertThatThrownBy(() -> exercise(new LocalizedText("Back squat", ""), 0, 5, 120))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRefuse_whenAmountIs0() {
            assertThatThrownBy(() -> exercise(new LocalizedText("Back squat", ""), 3, 0, 120))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRefuse_whenRestIsNegative() {
            assertThatThrownBy(() -> exercise(new LocalizedText("Back squat", ""), 3, 5, -1))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldAccept_whenRestIs0AndNameIs100Characters() {
            Exercise exercise = exercise(new LocalizedText("a".repeat(100), "b".repeat(100)), 1, 1, 0);

            assertThat(exercise.getRestSeconds()).isZero();
            assertThat(exercise.getName().en()).hasSize(100);
        }

        @Test
        void shouldHaveNoOwner_whenNoneIsGiven() {
            assertThat(exercise("Back squat", "Przysiad ze sztangą").getOwner()).isEmpty();
        }
    }

    @Nested
    class Matches {

        private final Exercise squat = exercise("Back squat", "Przysiad ze sztangą");

        @ParameterizedTest
        @ValueSource(strings = {"squat", "BACK SQ", "  squat  ", "przysiad", "SZTANGĄ"})
        void shouldMatch_whenTextIsPartOfEitherNameIgnoringCaseAndSpaces(String text) {
            assertThat(squat.matches(text)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void shouldMatch_whenTextIsBlank(String text) {
            assertThat(squat.matches(text)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"%", "_", "squat back", "sztanga"})
        void shouldNotMatch_whenTextIsNotPartOfAName(String text) {
            assertThat(squat.matches(text)).isFalse();
        }

        @Test
        void shouldMatchLiterally_whenTextHasWildcardCharacters() {
            assertThat(exercise("100% effort_sprint", "").matches("% effort_")).isTrue();
        }

        @Test
        void shouldNotMatchPlainL_whenNameHasLWithStroke() {
            assertThat(exercise("Calf raise", "Wspięcia na łydki").matches("lydki")).isFalse();
        }
    }
}
