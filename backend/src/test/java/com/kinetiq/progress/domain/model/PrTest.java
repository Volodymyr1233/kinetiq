package com.kinetiq.progress.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId ROW = ExerciseId.random();
    private static final ExerciseId PLANK = ExerciseId.random();
    private static final LocalDate MONDAY = LocalDate.parse("2026-11-09");
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final LocalDate WEDNESDAY = MONDAY.plusDays(2);
    private static final LocalDate FRIDAY = MONDAY.plusDays(4);

    // the top set of the exercise in the session log of that date; loadKg is null for a set without a load
    private static LoggedExercise row(LocalDate date, ExerciseId exercise, String loadKg, int amount) {
        return new LoggedExercise(
                date, exercise, new TopSet(loadKg == null ? null : new BigDecimal(loadKg), amount, 8), 8, 1);
    }

    private static Pr pr(ExerciseId exercise, String loadKg, int amount) {
        return new Pr(exercise, new TopSet(loadKg == null ? null : new BigDecimal(loadKg), amount, 8));
    }

    @Test
    void shouldBeAPr_whenTheSetBeatsEveryEarlierOne() {
        var logged = List.of(row(MONDAY, SQUAT, "100.0", 5), row(WEDNESDAY, SQUAT, "102.5", 5));

        assertThat(Pr.on(WEDNESDAY, logged)).containsExactly(pr(SQUAT, "102.5", 5));
    }

    @Test
    void shouldNotBeAPr_whenTheSetOnlyEqualsTheEarlierBest() {
        var logged = List.of(
                row(MONDAY, SQUAT, "100.0", 5), row(TUESDAY, SQUAT, "102.5", 5), row(WEDNESDAY, SQUAT, "102.5", 5));

        assertThat(Pr.on(WEDNESDAY, logged)).isEmpty();
    }

    @Test
    void shouldNotBeAPr_whenTheEarlierBestWasReachedTwiceAndIsOnlyEqualled() {
        var logged = List.of(
                row(MONDAY, SQUAT, "102.5", 5), row(TUESDAY, SQUAT, "102.5", 5), row(WEDNESDAY, SQUAT, "102.5", 5));

        assertThat(Pr.on(WEDNESDAY, logged)).isEmpty();
    }

    @Test
    void shouldBeAPr_whenTheLoadIsTheSameAndTheRepetitionsMore() {
        var logged = List.of(row(MONDAY, SQUAT, "100.0", 5), row(WEDNESDAY, SQUAT, "100.0", 6));

        assertThat(Pr.on(WEDNESDAY, logged)).containsExactly(pr(SQUAT, "100.0", 6));
    }

    @Test
    void shouldBeAPr_whenASetWithALoadFollowsSetsWithout() {
        var logged = List.of(row(MONDAY, ROW, null, 12), row(WEDNESDAY, ROW, "10.0", 5));

        assertThat(Pr.on(WEDNESDAY, logged)).containsExactly(pr(ROW, "10.0", 5));
    }

    @Test
    void shouldNotBeAPr_whenASetWithoutALoadFollowsOneWith() {
        var logged = List.of(row(MONDAY, ROW, "10.0", 5), row(WEDNESDAY, ROW, null, 20));

        assertThat(Pr.on(WEDNESDAY, logged)).isEmpty();
    }

    @Test
    void shouldCompareRepetitionsOrSeconds_whenNoSetHasALoad() {
        var logged = List.of(
                row(MONDAY, PLANK, null, 60), row(WEDNESDAY, PLANK, null, 75), row(FRIDAY, PLANK, null, 75));

        assertThat(Pr.on(WEDNESDAY, logged)).containsExactly(pr(PLANK, null, 75));
        assertThat(Pr.on(FRIDAY, logged)).isEmpty();
    }

    @Test
    void shouldNotBeAPr_whenTheExerciseIsLoggedForTheFirstTime() {
        var logged = List.of(row(WEDNESDAY, SQUAT, "102.5", 5));

        assertThat(Pr.on(WEDNESDAY, logged)).isEmpty();
    }

    @Test
    void shouldKeepThePr_whenALaterSessionBeatsIt() {
        var logged = List.of(
                row(MONDAY, SQUAT, "100.0", 5), row(WEDNESDAY, SQUAT, "102.5", 5), row(FRIDAY, SQUAT, "105.0", 5));

        assertThat(Pr.on(WEDNESDAY, logged)).containsExactly(pr(SQUAT, "102.5", 5));
    }

    @Test
    void shouldJudgeEachExerciseOfTheSessionApart() {
        var logged = List.of(
                row(MONDAY, SQUAT, "100.0", 5),
                row(MONDAY, ROW, "50.0", 8),
                row(WEDNESDAY, SQUAT, "102.5", 5),
                row(WEDNESDAY, ROW, "50.0", 8));

        assertThat(Pr.on(WEDNESDAY, logged)).containsExactly(pr(SQUAT, "102.5", 5));
    }
}
