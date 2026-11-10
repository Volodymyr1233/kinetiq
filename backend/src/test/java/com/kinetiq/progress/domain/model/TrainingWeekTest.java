package com.kinetiq.progress.domain.model;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.progress.domain.model.TrainingWeek.ExerciseWeek;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TrainingWeekTest {

    private static final LocalDate FIRST_MONDAY = LocalDate.parse("2026-10-05");
    private static final ExerciseId SQUAT = ExerciseId.random();

    // sessions on Monday, Wednesday and Friday, published on the Wednesday of week 1
    private static final Plan PLAN = new Plan(
            PlanId.random(),
            AccountId.random(),
            Goal.BUILD_MUSCLE,
            8,
            FIRST_MONDAY,
            FIRST_MONDAY.plusDays(2),
            Stream.of(MONDAY, WEDNESDAY, FRIDAY)
                    .map(day -> new Session("Session", day, List.of()))
                    .toList(),
            0,
            null);

    private static LoggedExercise row(String date, String loadKg, int amount, int rpe, int rpeSum, int sets) {
        return new LoggedExercise(
                LocalDate.parse(date), SQUAT, new TopSet(new BigDecimal(loadKg), amount, rpe), rpeSum, sets);
    }

    private static LoggedExercise row(String date, int rpe) {
        return row(date, "100.0", 5, rpe, rpe, 1);
    }

    private static Set<LocalDate> days(String... dates) {
        return Stream.of(dates).map(LocalDate::parse).collect(Collectors.toSet());
    }

    @Nested
    class Of {

        @Test
        void shouldPlanOnlyTheDaysFromThePublishDay_inTheFirstWeek() {
            List<TrainingWeek> weeks = TrainingWeek.of(PLAN, 2, false, List.of(), Set.of());

            assertThat(weeks).extracting(TrainingWeek::number).containsExactly(1, 2);
            assertThat(weeks).extracting(TrainingWeek::monday).containsExactly(FIRST_MONDAY, FIRST_MONDAY.plusWeeks(1));
            assertThat(weeks).extracting(TrainingWeek::planned).containsExactly(2, 3);
        }

        @Test
        void shouldCountOnlyFinishedSessionLogsOnPlannedDays_asDone() {
            // Monday of week 1 is before the publish day, Tuesday of week 2 has no session, Friday is not finished
            List<TrainingWeek> weeks = TrainingWeek.of(
                    PLAN,
                    2,
                    false,
                    List.of(row("2026-10-09", 8)),
                    days("2026-10-05", "2026-10-07", "2026-10-12", "2026-10-13"));

            assertThat(weeks).extracting(TrainingWeek::done).containsExactly(1, 1);
        }

        @Test
        void shouldCountASundayRowInItsWeek_andTheNextMondaysInTheNext() {
            List<TrainingWeek> weeks =
                    TrainingWeek.of(PLAN, 2, false, List.of(row("2026-10-11", 7), row("2026-10-12", 9)), Set.of());

            assertThat(weeks).extracting(TrainingWeek::rpeSum).containsExactly(7, 9);
            assertThat(weeks).extracting(TrainingWeek::sets).containsExactly(1, 1);
        }

        @Test
        void shouldKeepTheBestTopSetOfTheWeek_andTheEarlierOfTwoEqualOnes() {
            List<TrainingWeek> weeks = TrainingWeek.of(
                    PLAN,
                    1,
                    false,
                    List.of(
                            row("2026-10-07", "100.0", 8, 8, 16, 2),
                            row("2026-10-09", "105.0", 5, 9, 9, 1),
                            row("2026-10-11", "105.0", 5, 7, 7, 1)),
                    Set.of());

            assertThat(weeks.getFirst().exercises())
                    .containsExactly(Map.entry(
                            SQUAT, new ExerciseWeek(new TopSet(new BigDecimal("105.0"), 5, 9), 32, 4)));
        }

        @Test
        void shouldMarkOnlyTheLastWeekRunning_whenAsked() {
            assertThat(TrainingWeek.of(PLAN, 3, true, List.of(), Set.of()))
                    .extracting(TrainingWeek::running)
                    .containsExactly(false, false, true);
            assertThat(TrainingWeek.of(PLAN, 3, false, List.of(), Set.of()))
                    .extracting(TrainingWeek::running)
                    .containsExactly(false, false, false);
        }
    }

    @Nested
    class AverageRpe {

        @Test
        void shouldCountEverySetOfTheWeek_finishedOrNot_toOneDecimalHalfUp() {
            // 29 / 4 = 7.25, which half up makes 7.3
            List<TrainingWeek> weeks = TrainingWeek.of(
                    PLAN,
                    1,
                    false,
                    List.of(row("2026-10-07", "100.0", 5, 8, 15, 2), row("2026-10-09", "100.0", 5, 8, 14, 2)),
                    days("2026-10-07"));

            assertThat(weeks.getFirst().averageRpe()).contains(new BigDecimal("7.3"));
        }

        @Test
        void shouldBeEmpty_whenTheWeekHasNoSets() {
            assertThat(TrainingWeek.of(PLAN, 1, false, List.of(), Set.of()).getFirst().averageRpe())
                    .isEmpty();
        }
    }
}
