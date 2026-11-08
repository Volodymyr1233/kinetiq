package com.kinetiq.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.readiness.domain.model.Readiness.Band;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReadinessTest {

    @Test
    void shouldFollowTheMeanOfTheThreeParts_forEveryCombinationOfAnswers() {
        for (int sleep = 1; sleep <= 5; sleep++) {
            for (int soreness = 1; soreness <= 5; soreness++) {
                for (int stress = 1; stress <= 5; stress++) {
                    double mean = ((sleep - 1) / 4.0 + (5 - soreness) / 4.0 + (5 - stress) / 4.0) / 3;
                    long expected = Math.round(mean * 100);
                    Band band = expected >= 70 ? Band.READY : expected >= 45 ? Band.TAKE_IT_EASIER : Band.RECOVERY_DAY;

                    Readiness readiness = Readiness.of(new Answers(sleep, soreness, stress));

                    assertThat(readiness)
                            .as("%d/%d/%d", sleep, soreness, stress)
                            .isEqualTo(new Readiness((int) expected, band));
                }
            }
        }
    }

    @ParameterizedTest(name = "{0}/{1}/{2} gives {3} {4}")
    @CsvSource({
        "2, 4, 3,  33, RECOVERY_DAY",
        "4, 2, 1,  83, READY",
        "5, 1, 1, 100, READY",
        "1, 5, 5,   0, RECOVERY_DAY",
        "3, 3, 4,  42, RECOVERY_DAY",
        "3, 3, 3,  50, TAKE_IT_EASIER",
        "4, 2, 3,  67, TAKE_IT_EASIER",
        "4, 2, 2,  75, READY"
    })
    void shouldGiveTheScoreAndBand_ofTheNamedCasesAndBandEdges(
            int sleep, int soreness, int stress, int score, Band band) {
        assertThat(Readiness.of(new Answers(sleep, soreness, stress))).isEqualTo(new Readiness(score, band));
    }
}
