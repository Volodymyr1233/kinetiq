package com.kinetiq.planchange.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.LoggedSet;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HighRpeRuleTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId ROW = ExerciseId.random();

    private final HighRpeRule rule = new HighRpeRule();

    private static List<LoggedSet> sets(ExerciseId exercise, int count, int rpe) {
        List<LoggedSet> sets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sets.add(new LoggedSet(exercise, null, rpe));
        }
        return sets;
    }

    private static WeekTraining week(String monday, List<LoggedSet> sets) {
        return new WeekTraining(LocalDate.parse(monday), 3, 3, 3, sets, List.of());
    }

    private Optional<Finding> evaluate(List<LoggedSet> first, List<LoggedSet> second) {
        return rule.evaluate(List.of(week("2026-11-02", first), week("2026-11-09", second)));
    }

    @Test
    void shouldNotFire_whenTheMeanIsExactly8InBothWeeks() {
        assertThat(evaluate(sets(SQUAT, 10, 8), sets(SQUAT, 10, 8))).isEmpty();
    }

    @Test
    void shouldFire_whenTheMeanIsJustAbove8InBothWeeks() {
        // 201 over 25 sets is 8.04, which a mean rounded to one decimal would read as 8.0
        List<LoggedSet> week = new ArrayList<>(sets(SQUAT, 24, 8));
        week.add(new LoggedSet(SQUAT, null, 9));

        assertThat(evaluate(week, week)).isPresent();
    }

    @Test
    void shouldNotFire_whenOnlyOneWeekIsAbove8() {
        assertThat(evaluate(sets(SQUAT, 10, 9), sets(SQUAT, 10, 8))).isEmpty();
        assertThat(evaluate(sets(SQUAT, 10, 8), sets(SQUAT, 10, 9))).isEmpty();
    }

    @Test
    void shouldNotFire_whenAWeekHasNoSets() {
        assertThat(evaluate(sets(SQUAT, 10, 9), List.of())).isEmpty();
    }

    @Test
    void shouldListOnlyTheExercisesWhoseMeanIsAbove8() {
        List<LoggedSet> week = new ArrayList<>(sets(SQUAT, 6, 10));
        week.addAll(sets(ROW, 4, 7));
        // the week's mean is 8.8, the row's own mean is 7

        Finding finding = evaluate(week, week).orElseThrow();

        assertThat(finding.rule()).isEqualTo(Rule.HIGH_RPE);
        assertThat(finding.exercises()).containsExactly(SQUAT);
    }

    @Test
    void shouldListAnExerciseOnItsMeanOverTheWholeWindow() {
        // the row is 9 in the first week and 8 in the second: mean 8.5 over the window
        List<LoggedSet> first = new ArrayList<>(sets(ROW, 5, 9));
        first.addAll(sets(SQUAT, 5, 9));
        List<LoggedSet> second = new ArrayList<>(sets(ROW, 5, 8));
        second.addAll(sets(SQUAT, 5, 10));

        assertThat(evaluate(first, second).orElseThrow().exercises()).containsExactlyInAnyOrder(SQUAT, ROW);
    }

    @Test
    void shouldReportTheMeanRpeOfEveryWeek_asTheNumbersItCompared() {
        // 201 over 25 sets is 8.04; 26 sets of 9 and 4 of 8 over 30 is 8.867
        List<LoggedSet> first = new ArrayList<>(sets(SQUAT, 24, 8));
        first.add(new LoggedSet(SQUAT, null, 9));
        List<LoggedSet> second = new ArrayList<>(sets(SQUAT, 26, 9));
        second.addAll(sets(ROW, 4, 8));

        assertThat(evaluate(first, second).orElseThrow().figures())
                .containsExactly(
                        Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-11-02"), new BigDecimal("8.040")),
                        Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-11-09"), new BigDecimal("8.867")));
    }
}
