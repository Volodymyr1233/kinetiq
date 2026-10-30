package com.kinetiq.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.common.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

    // the answers are few enough to try every one: 5 x 5 x 5
    @Nested
    class EveryCheckIn {

        private static void forEachCheckIn(CheckInAssertion assertion) {
            for (int sleep = 1; sleep <= 5; sleep++) {
                for (int soreness = 1; soreness <= 5; soreness++) {
                    for (int stress = 1; stress <= 5; stress++) {
                        assertion.check(sleep, soreness, stress);
                    }
                }
            }
        }

        private interface CheckInAssertion {
            void check(int sleep, int soreness, int stress);
        }

        @Test
        void shouldScoreFromZeroToHundred_whateverTheAnswers() {
            forEachCheckIn((sleep, soreness, stress) -> assertThat(
                            Readiness.fromCheckIn(sleep, soreness, stress).score())
                    .isBetween(0, 100));
        }

        @Test
        void shouldNeverLowerTheScore_whenSleepImproves() {
            forEachCheckIn((sleep, soreness, stress) -> {
                if (sleep < 5) {
                    assertThat(Readiness.fromCheckIn(sleep + 1, soreness, stress).score())
                            .isGreaterThan(Readiness.fromCheckIn(sleep, soreness, stress).score());
                }
            });
        }

        @Test
        void shouldNeverRaiseTheScore_whenSorenessOrStressGrows() {
            forEachCheckIn((sleep, soreness, stress) -> {
                if (soreness < 5) {
                    assertThat(Readiness.fromCheckIn(sleep, soreness + 1, stress).score())
                            .isLessThan(Readiness.fromCheckIn(sleep, soreness, stress).score());
                }
                if (stress < 5) {
                    assertThat(Readiness.fromCheckIn(sleep, soreness, stress + 1).score())
                            .isLessThan(Readiness.fromCheckIn(sleep, soreness, stress).score());
                }
            });
        }

        // no answers give exactly 45 or 70, so the edges are met through the nearest scores
        @ParameterizedTest
        @CsvSource({
            "2, 3, 3, 42, RECOVERY_DAY",
            "3, 3, 3, 50, TAKE_IT_EASIER",
            "4, 3, 3, 58, TAKE_IT_EASIER",
            "4, 2, 3, 67, TAKE_IT_EASIER",
            "4, 2, 2, 75, READY",
        })
        void shouldPlaceTheBand_whenAnswersLandNearAnEdge(
                int sleep, int soreness, int stress, int score, ReadinessBand band) {
            Readiness readiness = Readiness.fromCheckIn(sleep, soreness, stress);

            assertThat(readiness.score()).isEqualTo(score);
            assertThat(readiness.band()).isEqualTo(band);
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
