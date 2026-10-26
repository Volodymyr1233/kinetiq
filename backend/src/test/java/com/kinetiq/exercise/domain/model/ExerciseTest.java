package com.kinetiq.exercise.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExerciseTest {

    private static final LocalizedText EQUIPMENT = new LocalizedText("Barbell", "Sztanga");
    private static final LocalizedText CUES = new LocalizedText("Brace first.", "Najpierw napnij brzuch.");
    private static final AccountId OWNER = AccountId.random();

    private static Exercise owned(AccountId owner) {
        return new Exercise(
                ExerciseId.random(),
                owner,
                new LocalizedText("Wall sit", ""),
                Category.LEGS,
                EQUIPMENT,
                Measure.SECONDS,
                3,
                45,
                90,
                CUES);
    }

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

        @ParameterizedTest
        @CsvSource({"0, 5, 120", "21, 5, 120", "3, 0, 120", "3, 3601, 120", "3, 5, -1", "3, 5, 601"})
        void shouldRefuse_whenSetsAmountOrRestIsOutOfRange(int sets, int amount, int rest) {
            assertThatThrownBy(() -> exercise(new LocalizedText("Back squat", ""), sets, amount, rest))
                    .isInstanceOf(InvalidValueException.class);
        }

        @ParameterizedTest
        @CsvSource({"1, 1, 0", "20, 3600, 600"})
        void shouldAccept_whenSetsAmountAndRestAreAtTheirEdges(int sets, int amount, int rest) {
            Exercise exercise = exercise(new LocalizedText("Back squat", ""), sets, amount, rest);

            assertThat(exercise.getDefaultSets()).isEqualTo(sets);
            assertThat(exercise.getDefaultAmount()).isEqualTo(amount);
            assertThat(exercise.getRestSeconds()).isEqualTo(rest);
        }

        @Test
        void shouldAccept_whenNameIs100Characters() {
            Exercise exercise = exercise(new LocalizedText("a".repeat(100), "b".repeat(100)), 1, 1, 0);

            assertThat(exercise.getName().en()).hasSize(100);
        }

        @Test
        void shouldHaveNoOwner_whenNoneIsGiven() {
            assertThat(exercise("Back squat", "Przysiad ze sztangą").getOwner()).isEmpty();
        }
    }

    @Nested
    class IsOwnedBy {

        @Test
        void shouldBeTrueOnlyForOwner_whenExerciseIsOwn() {
            Exercise own = owned(OWNER);

            assertThat(own.isOwnedBy(OWNER)).isTrue();
            assertThat(own.isOwnedBy(AccountId.random())).isFalse();
            assertThat(own.isStarter()).isFalse();
        }

        @Test
        void shouldBeFalseForEveryone_whenExerciseIsStarter() {
            Exercise starter = exercise("Back squat", "Przysiad ze sztangą");

            assertThat(starter.isOwnedBy(OWNER)).isFalse();
            assertThat(starter.isStarter()).isTrue();
        }
    }

    @Nested
    class Edit {

        private final Exercise own = owned(OWNER);

        @Test
        void shouldKeepIdOwnerAndMeasure_whenValuesChange() {
            Exercise edited = own.edit(
                    new LocalizedText("Goblet squat", ""), Category.CORE, new LocalizedText("", ""), 4, 8, 60, CUES);

            assertThat(edited.getId()).isEqualTo(own.getId());
            assertThat(edited.isOwnedBy(OWNER)).isTrue();
            assertThat(edited.getMeasure()).isEqualTo(Measure.SECONDS);
            assertThat(edited.getName().en()).isEqualTo("Goblet squat");
            assertThat(edited.getCategory()).isEqualTo(Category.CORE);
            assertThat(edited.getDefaultSets()).isEqualTo(4);
            assertThat(edited.getDefaultAmount()).isEqualTo(8);
            assertThat(edited.getRestSeconds()).isEqualTo(60);
        }

        @Test
        void shouldRefuse_whenNewValueIsOutOfRange() {
            assertThatThrownBy(() -> own.edit(own.getName(), Category.LEGS, EQUIPMENT, 21, 8, 60, CUES))
                    .isInstanceOf(InvalidValueException.class);
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
