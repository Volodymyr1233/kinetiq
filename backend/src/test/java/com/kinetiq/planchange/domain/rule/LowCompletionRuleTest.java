package com.kinetiq.planchange.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LowCompletionRuleTest {

    private final LowCompletionRule rule = new LowCompletionRule();

    private static WeekTraining week(int index, int inPlan, int planned, int finished) {
        return new WeekTraining(LocalDate.parse("2026-11-02").plusWeeks(index), inPlan, planned, finished, List.of(), List.of());
    }

    private boolean fires(WeekTraining first, WeekTraining second) {
        return rule.evaluate(List.of(first, second)).isPresent();
    }

    @Test
    void shouldFire_when4Of6SessionsWereFinished() {
        assertThat(fires(week(0, 3, 3, 2), week(1, 3, 3, 2))).isTrue();
    }

    @Test
    void shouldNotFire_when5Of6SessionsWereFinished() {
        assertThat(fires(week(0, 3, 3, 3), week(1, 3, 3, 2))).isFalse();
    }

    @Test
    void shouldNotFire_whenExactly70PercentWasFinished() {
        assertThat(fires(week(0, 5, 5, 4), week(1, 5, 5, 3))).isFalse();
    }

    @Test
    void shouldFire_whenJustBelow70PercentWasFinished() {
        // 13 of 19 is 68.4%
        assertThat(fires(week(0, 5, 10, 7), week(1, 5, 9, 6))).isTrue();
    }

    @Test
    void shouldNeverFire_forAPlanOf2SessionsAWeek() {
        assertThat(fires(week(0, 2, 2, 0), week(1, 2, 2, 0))).isFalse();
    }

    @Test
    void shouldNeverFire_whenOneWeekOfTheWindowHad2SessionsInThePlan() {
        assertThat(fires(week(0, 2, 2, 0), week(1, 3, 3, 0))).isFalse();
    }

    @Test
    void shouldCountOnlyThePlannedDays_whenThePlanWasPublishedMidWeek() {
        // a plan of 3 published on a Friday has 1 planned session in its first week
        assertThat(fires(week(0, 3, 1, 1), week(1, 3, 3, 3))).isFalse();
        assertThat(fires(week(0, 3, 1, 0), week(1, 3, 3, 2))).isTrue();
    }

    @Test
    void shouldNotFire_whenNothingWasPlanned() {
        assertThat(fires(week(0, 3, 0, 0), week(1, 3, 0, 0))).isFalse();
    }

    @Test
    void shouldReportThePlannedAndFinishedSessionsOfEveryWeek_asTheNumbersItCompared() {
        WeekTraining first = week(0, 3, 3, 2);
        WeekTraining second = week(1, 3, 2, 1);

        assertThat(rule.evaluate(List.of(first, second)).orElseThrow().figures())
                .containsExactly(
                        Figure.ofWeek(FigureKind.SESSIONS_PLANNED, first.monday(), BigDecimal.valueOf(3)),
                        Figure.ofWeek(FigureKind.SESSIONS_FINISHED, first.monday(), BigDecimal.valueOf(2)),
                        Figure.ofWeek(FigureKind.SESSIONS_PLANNED, second.monday(), BigDecimal.valueOf(2)),
                        Figure.ofWeek(FigureKind.SESSIONS_FINISHED, second.monday(), BigDecimal.valueOf(1)));
    }
}
