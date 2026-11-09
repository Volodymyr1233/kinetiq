package com.kinetiq.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.WeightUnit;
import com.kinetiq.exercise.domain.model.Measure;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AdjustmentTest {

    private static final Answers RECOVERY = new Answers(2, 4, 3);

    private static Target load(String kg, int amount, int rpe) {
        return new Target(new BigDecimal(kg), amount, rpe);
    }

    @Nested
    class Lighter {

        @ParameterizedTest(name = "{0} kg gives {1} kg")
        @CsvSource({
            "100.0, 90.0",
            "80.0, 72.0",
            // 92.25 is halfway between 92 and 92.5
            "102.5, 92.0",
            "20.0, 18.0",
            // 2.25 is halfway between 2 and 2.5
            "2.5, 2.0",
            // 0.45 would round back to 0.5
            "0.5, 0.5",
            "0.6, 0.5",
            // 0.18 rounds to no load, which a load never becomes
            "0.2, 0.2"
        })
        void shouldTakeTenPercentOffRoundedToHalfKilograms_whenTheClientUsesKilograms(String planned, String lighter) {
            Target result = Adjustment.lighter(load(planned, 5, 8), Measure.REPS, WeightUnit.KG);

            assertThat(result).isEqualTo(load(lighter, 5, 7));
        }

        @ParameterizedTest(name = "{0} kg gives {1} kg")
        @CsvSource({
            // 199.96 lb: 179.96 rounds to 180 lb, kept as 81.6 kg
            "90.7, 81.6",
            // 2.2 lb: 1.98 rounds to 2 lb
            "1.0, 0.9",
            // 1.1 lb: 0.99 rounds to 1 lb, which reads 0.5 kg again
            "0.5, 0.5",
            // 50.0 lb: 45.0 lb exactly
            "22.7, 20.4"
        })
        void shouldTakeTenPercentOffRoundedToWholePounds_whenTheClientUsesPounds(String planned, String lighter) {
            Target result = Adjustment.lighter(load(planned, 5, 8), Measure.REPS, WeightUnit.LB);

            assertThat(result).isEqualTo(load(lighter, 5, 7));
        }

        @Test
        void shouldKeepRepetitionsAndSeconds_whenThereIsALoad() {
            assertThat(Adjustment.lighter(load("20.0", 45, 7), Measure.SECONDS, WeightUnit.KG))
                    .isEqualTo(load("18.0", 45, 6));
        }

        @ParameterizedTest(name = "RPE {0} gives {1}")
        @CsvSource({"10, 9", "8, 7", "6, 5", "5, 5"})
        void shouldLowerRpeByOneButNotBelowFive(int planned, int lighter) {
            assertThat(Adjustment.lighter(load("50.0", 5, planned), Measure.REPS, WeightUnit.KG).rpe())
                    .isEqualTo(lighter);
        }

        @ParameterizedTest(name = "{0} reps give {1}")
        @CsvSource({"12, 11", "5, 4", "1, 1"})
        void shouldDropOneRepetition_whenThereIsNoLoad(int planned, int lighter) {
            assertThat(Adjustment.lighter(new Target(null, planned, 8), Measure.REPS, WeightUnit.KG))
                    .isEqualTo(new Target(null, lighter, 7));
        }

        @ParameterizedTest(name = "{0} s give {1} s")
        @CsvSource({"45, 40", "30, 27", "60, 54", "5, 4", "1, 1"})
        void shouldDropTenPercentOfTheSecondsRoundedDown_whenThereIsNoLoad(int planned, int lighter) {
            assertThat(Adjustment.lighter(new Target(null, planned, 7), Measure.SECONDS, WeightUnit.KG))
                    .isEqualTo(new Target(null, lighter, 6));
        }
    }

    @Nested
    class Of {

        private final List<Planned> session = List.of(
                new Planned(load("100.0", 5, 8), Measure.REPS),
                new Planned(new Target(null, 12, 8), Measure.REPS),
                new Planned(new Target(null, 45, 7), Measure.SECONDS));

        @Test
        void shouldGiveTheLighterTargetsInPlanOrder_whenItIsARecoveryDay() {
            Adjustment adjustment =
                    Adjustment.of(RECOVERY, true, session, WeightUnit.KG).orElseThrow();

            assertThat(adjustment.lighter())
                    .containsExactly(load("90.0", 5, 7), new Target(null, 11, 7), new Target(null, 40, 6));
            assertThat(adjustment.inUse()).isTrue();
        }

        @ParameterizedTest(name = "{0}/{1}/{2}")
        @CsvSource({"4, 2, 1", "3, 3, 3"})
        void shouldGiveNone_whenItIsNotARecoveryDay(int sleep, int soreness, int stress) {
            assertThat(Adjustment.of(new Answers(sleep, soreness, stress), true, session, WeightUnit.KG))
                    .isEmpty();
        }

        @Test
        void shouldGiveNone_whenNoTargetCanGetLighter() {
            List<Planned> atTheFloor = List.of(
                    new Planned(new Target(null, 1, 5), Measure.REPS),
                    new Planned(load("0.5", 8, 5), Measure.REPS));

            assertThat(Adjustment.of(RECOVERY, true, atTheFloor, WeightUnit.KG)).isEmpty();
        }

        @ParameterizedTest(name = "{0}/{1}/{2} names {3}")
        @CsvSource({
            "2, 4, 3, 'SLEEP,SORENESS'",
            "1, 5, 5, 'SLEEP,SORENESS,STRESS'",
            "3, 4, 5, 'SORENESS,STRESS'",
            "1, 3, 3, 'SLEEP'"
        })
        void shouldNameTheAnswersThatCausedIt_inTheirOrder(int sleep, int soreness, int stress, String causes) {
            Adjustment adjustment = Adjustment.of(new Answers(sleep, soreness, stress), true, session, WeightUnit.KG)
                    .orElseThrow();

            assertThat(adjustment.causes())
                    .containsExactlyElementsOf(List.of(causes.split(",")).stream()
                            .map(Cause::valueOf)
                            .toList());
        }

        @Test
        void shouldUseThePlannedTargets_whenTheClientChoseTheOriginalOnes() {
            Adjustment adjustment =
                    Adjustment.of(RECOVERY, false, session, WeightUnit.KG).orElseThrow();

            assertThat(adjustment.inUse(0, session.getFirst().target())).isEqualTo(load("100.0", 5, 8));
        }

        @Test
        void shouldUseTheLighterTargets_whileTheyAreInUse() {
            Adjustment adjustment = Adjustment.of(RECOVERY, true, session, WeightUnit.KG).orElseThrow();

            assertThat(adjustment.inUse(0, session.getFirst().target())).isEqualTo(load("90.0", 5, 7));
        }
    }
}
