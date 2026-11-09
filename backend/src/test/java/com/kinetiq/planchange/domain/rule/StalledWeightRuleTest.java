package com.kinetiq.planchange.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
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

class StalledWeightRuleTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId BENCH = ExerciseId.random();
    private static final ExerciseId PLANK = ExerciseId.random();

    private final StalledWeightRule rule = new StalledWeightRule();

    private static LoggedSet set(ExerciseId exercise, String load, int rpe) {
        return new LoggedSet(exercise, load == null ? null : new BigDecimal(load), rpe);
    }

    private static WeekTraining week(int index, LoggedSet... sets) {
        return new WeekTraining(LocalDate.parse("2026-10-19").plusWeeks(index), 3, 3, 3, List.of(sets));
    }

    // one exercise, one set a week, at the given loads and RPE
    private Optional<Finding> evaluate(ExerciseId exercise, int rpe, String first, String second, String third) {
        return rule.evaluate(List.of(
                week(0, set(exercise, first, rpe)),
                week(1, set(exercise, second, rpe)),
                week(2, set(exercise, third, rpe))));
    }

    @Test
    void shouldNotFire_whenTheBestLoadRoseExactly5Percent() {
        assertThat(evaluate(SQUAT, 7, "100.0", "102.0", "105.0")).isEmpty();
    }

    @Test
    void shouldFire_whenTheBestLoadRoseLessThan5Percent() {
        Finding finding = evaluate(SQUAT, 8, "100.0", "101.0", "104.9").orElseThrow();

        assertThat(finding.rule()).isEqualTo(Rule.STALLED_WEIGHT);
        assertThat(finding.exercises()).containsExactly(SQUAT);
    }

    @Test
    void shouldFire_whenTheBestLoadFell() {
        assertThat(evaluate(SQUAT, 7, "100.0", "95.0", "90.0")).isPresent();
    }

    @Test
    void shouldNotFire_whenTheMeanRpeIsAbove8_butFireAtExactly8() {
        assertThat(evaluate(SQUAT, 9, "100.0", "100.0", "100.0")).isEmpty();
        assertThat(evaluate(SQUAT, 8, "100.0", "100.0", "100.0")).isPresent();
    }

    @Test
    void shouldJudgeTheMeanRpeOverTheWholeWindow_notWeekByWeek() {
        // 9, 8 and 7 average exactly 8 over the window, though the first week alone is above it
        assertThat(rule.evaluate(List.of(
                        week(0, set(SQUAT, "100.0", 9)),
                        week(1, set(SQUAT, "100.0", 8)),
                        week(2, set(SQUAT, "100.0", 7)))))
                .isPresent();
    }

    @Test
    void shouldNotFire_whenTheMeanRpeIsJustAbove8() {
        // 233 over 29 sets is 8.03, which a mean rounded to one decimal would read as 8.0 and let through
        List<LoggedSet> first = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            first.add(set(SQUAT, "100.0", 8));
        }
        first.add(set(SQUAT, "100.0", 9));

        assertThat(rule.evaluate(List.of(
                        new WeekTraining(LocalDate.parse("2026-10-19"), 3, 3, 3, first),
                        week(1, set(SQUAT, "100.0", 8), set(SQUAT, "100.0", 8)),
                        week(2, set(SQUAT, "100.0", 8)))))
                .isEmpty();
    }

    @Test
    void shouldNotFire_whenTheExerciseWasLoggedInOnly2OfTheWeeks() {
        assertThat(rule.evaluate(List.of(
                        week(0, set(SQUAT, "100.0", 7)),
                        week(1, set(BENCH, "60.0", 7)),
                        week(2, set(SQUAT, "100.0", 7)))))
                .isEmpty();
    }

    @Test
    void shouldIgnoreAnExerciseWithoutALoad() {
        assertThat(evaluate(PLANK, 6, null, null, null)).isEmpty();
    }

    @Test
    void shouldListOnlyTheStalledExercises() {
        Finding finding = rule.evaluate(List.of(
                        week(0, set(SQUAT, "100.0", 7), set(BENCH, "60.0", 7)),
                        week(1, set(SQUAT, "100.0", 7), set(BENCH, "65.0", 7)),
                        week(2, set(SQUAT, "100.0", 7), set(BENCH, "70.0", 7))))
                .orElseThrow();

        assertThat(finding.exercises()).containsExactly(SQUAT);
    }

    @Test
    void shouldUseTheHighestLoadOfTheWeek_asTheBestSet() {
        // a heavy single and a light back-off set in week 1; week 3 only has the heavy single, up 2%
        Optional<Finding> finding = rule.evaluate(List.of(
                week(0, set(SQUAT, "100.0", 7), set(SQUAT, "80.0", 7)),
                week(1, set(SQUAT, "100.0", 7)),
                week(2, set(SQUAT, "102.0", 7))));

        assertThat(finding).isPresent();
    }
}
