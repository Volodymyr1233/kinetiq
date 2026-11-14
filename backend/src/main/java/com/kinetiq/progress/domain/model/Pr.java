package com.kinetiq.progress.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * An exercise whose top set in one session log beats every set of it the client logged before, by the top set order.
 * A set that only equals the earlier best is not a PR, and neither is the first log of an exercise.
 */
public record Pr(ExerciseId exercise, TopSet top) {

    public Pr {
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(top);
    }

    /**
     * The PRs of the session log dated {@code date}: each of its exercises with at least one earlier row, whose top set
     * is better than every one of them. Rows after {@code date} are not compared; the order of {@code logged} does not
     * matter.
     */
    public static List<Pr> on(LocalDate date, List<LoggedExercise> logged) {
        return logged.stream()
                .filter(row -> row.date().equals(date))
                .filter(row -> beatsEveryEarlierRow(row, logged))
                .map(row -> new Pr(row.exercise(), row.top()))
                .toList();
    }

    private static boolean beatsEveryEarlierRow(LoggedExercise row, List<LoggedExercise> logged) {
        List<TopSet> earlier = logged.stream()
                .filter(other -> other.exercise().equals(row.exercise())
                        && other.date().isBefore(row.date()))
                .map(LoggedExercise::top)
                .toList();
        return !earlier.isEmpty() && earlier.stream().allMatch(row.top()::isBetterThan);
    }
}
