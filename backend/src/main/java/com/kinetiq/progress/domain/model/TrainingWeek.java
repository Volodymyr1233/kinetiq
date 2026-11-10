package com.kinetiq.progress.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.model.Plan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One plan week of one client. {@code planned} counts the plan's sessions on days on or after the publish day and
 * {@code done} those with a finished session log; {@code rpeSum} and {@code sets} cover every logged set of the week,
 * finished or not. {@code running} marks the current week of a plan that has not finished.
 */
public record TrainingWeek(
        int number,
        LocalDate monday,
        int planned,
        int done,
        boolean running,
        int rpeSum,
        int sets,
        Map<ExerciseId, ExerciseWeek> exercises) {

    private static final int DAYS_PER_WEEK = 7;
    private static final int RPE_DECIMALS = 1;

    public TrainingWeek {
        Objects.requireNonNull(monday);
        exercises = Map.copyOf(exercises);
    }

    /** One exercise in a week: its top set, the best of its sessions', and the sum and count of its RPE. */
    public record ExerciseWeek(TopSet top, int rpeSum, int sets) {}

    /** The mean RPE of the week's sets to one decimal, half up; empty without sets. */
    public Optional<BigDecimal> averageRpe() {
        if (sets == 0) {
            return Optional.empty();
        }
        return Optional.of(
                BigDecimal.valueOf(rpeSum).divide(BigDecimal.valueOf(sets), RPE_DECIMALS, RoundingMode.HALF_UP));
    }

    /**
     * Builds weeks 1 to {@code lastWeek} of a published plan, with the plan's sessions as they are when read. Each row
     * counts in the week of its date, and rows outside those weeks are left out. {@code logged} is oldest first, as
     * {@code TrainingLog} returns it, so of two equal top sets the earlier stays. Only the last week can be running.
     */
    public static List<TrainingWeek> of(
            Plan plan, int lastWeek, boolean lastRunning, List<LoggedExercise> logged, Set<LocalDate> finishedDays) {
        LocalDate firstMonday = plan.getFirstWeek().orElseThrow();
        LocalDate publishedOn = plan.getPublishedOn().orElseThrow();
        Map<Integer, List<LoggedExercise>> byWeek =
                logged.stream().collect(Collectors.groupingBy(row -> weekOf(firstMonday, row.date())));
        List<TrainingWeek> weeks = new ArrayList<>();
        for (int number = 1; number <= lastWeek; number++) {
            LocalDate monday = firstMonday.plusWeeks(number - 1L);
            List<LocalDate> planned = plan.getSessions().stream()
                    .map(session -> monday.plusDays(session.day().getValue() - 1L))
                    .filter(day -> !day.isBefore(publishedOn))
                    .toList();
            List<LoggedExercise> rows = byWeek.getOrDefault(number, List.of());
            Map<ExerciseId, ExerciseWeek> exercises = new HashMap<>();
            rows.forEach(row -> exercises.merge(
                    row.exercise(), new ExerciseWeek(row.top(), row.rpeSum(), row.sets()), TrainingWeek::add));
            weeks.add(new TrainingWeek(
                    number,
                    monday,
                    planned.size(),
                    (int) planned.stream().filter(finishedDays::contains).count(),
                    lastRunning && number == lastWeek,
                    rows.stream().mapToInt(LoggedExercise::rpeSum).sum(),
                    rows.stream().mapToInt(LoggedExercise::sets).sum(),
                    exercises));
        }
        return weeks;
    }

    // the earlier session's top set stays on a tie
    private static ExerciseWeek add(ExerciseWeek earlier, ExerciseWeek later) {
        return new ExerciseWeek(
                later.top().isBetterThan(earlier.top()) ? later.top() : earlier.top(),
                earlier.rpeSum() + later.rpeSum(),
                earlier.sets() + later.sets());
    }

    private static int weekOf(LocalDate firstMonday, LocalDate date) {
        return (int) Math.floorDiv(ChronoUnit.DAYS.between(firstMonday, date), DAYS_PER_WEEK) + 1;
    }
}
