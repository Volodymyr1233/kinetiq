package com.kinetiq.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.common.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ReadinessTest {

    @Nested
    class FromCheckIn {

        @ParameterizedTest
        @CsvSource({
            "2, 4, 3, 33",
            "4, 2, 1, 83",
            "1, 5, 5, 0",
            "5, 1, 1, 100",
            "3, 3, 3, 50",
        })
        void shouldScoreTheMeanOfTheThreeAnswers_whenAnswersAreValid(int sleep, int soreness, int stress, int score) {
            assertThat(Readiness.fromCheckIn(sleep, soreness, stress).score()).isEqualTo(score);
        }

        @ParameterizedTest
        @CsvSource({"0, 3, 3", "6, 3, 3", "3, 0, 3", "3, 6, 3", "3, 3, 0", "3, 3, 6"})
        void shouldReject_whenAnAnswerIsOutsideOneToFive(int sleep, int soreness, int stress) {
            assertThatThrownBy(() -> Readiness.fromCheckIn(sleep, soreness, stress))
                    .isInstanceOf(InvalidValueException.class);
        }
    }

    @Nested
    class Band {

        @ParameterizedTest
        @ValueSource(ints = {70, 100})
        void shouldBeReady_when70OrAbove(int score) {
            assertThat(new Readiness(score).band()).isEqualTo(ReadinessBand.READY);
        }

        @ParameterizedTest
        @ValueSource(ints = {45, 69})
        void shouldBeTakeItEasier_whenFrom45To69(int score) {
            assertThat(new Readiness(score).band()).isEqualTo(ReadinessBand.TAKE_IT_EASIER);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 44})
        void shouldBeRecoveryDay_whenBelow45(int score) {
            assertThat(new Readiness(score).band()).isEqualTo(ReadinessBand.RECOVERY_DAY);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void shouldReject_whenScoreIsOutsideZeroToHundred(int score) {
        assertThatThrownBy(() -> new Readiness(score)).isInstanceOf(InvalidValueException.class);
    }
}
