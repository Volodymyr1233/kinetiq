package com.kinetiq.progress.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.progress.domain.model.TopSet;
import com.kinetiq.progress.domain.model.TrainingWeek;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * What a client and their trainer read of the client's training over one period. {@code week} is the current plan
 * week, the last one once the plan has finished, and {@code weeks} the period's plan weeks, oldest first. The mean RPE
 * is absent without a set in the period; each change compares the last completed week with the week before it and is
 * absent when either has no value for it. {@code exercises} are those logged in the period, the one in the most
 * sessions first, then the most recently logged.
 */
@Getter
public class Progress {

    private final Goal goal;
    private final int week;
    private final boolean finished;
    private final List<TrainingWeek> weeks;
    private final int sessionsDone;
    private final int sessionsPlanned;

    @Getter(AccessLevel.NONE)
    private final BigDecimal averageRpe;

    @Getter(AccessLevel.NONE)
    private final Integer sessionsDoneChange;

    @Getter(AccessLevel.NONE)
    private final BigDecimal averageRpeChange;

    private final int weeksInARow;
    private final List<ExerciseProgress> exercises;

    /** {@code averageRpe}, {@code sessionsDoneChange} and {@code averageRpeChange} may be {@code null}. */
    public Progress(
            Goal goal,
            int week,
            boolean finished,
            List<TrainingWeek> weeks,
            int sessionsDone,
            int sessionsPlanned,
            BigDecimal averageRpe,
            Integer sessionsDoneChange,
            BigDecimal averageRpeChange,
            int weeksInARow,
            List<ExerciseProgress> exercises) {
        this.goal = Objects.requireNonNull(goal);
        this.week = week;
        this.finished = finished;
        this.weeks = List.copyOf(weeks);
        this.sessionsDone = sessionsDone;
        this.sessionsPlanned = sessionsPlanned;
        this.averageRpe = averageRpe;
        this.sessionsDoneChange = sessionsDoneChange;
        this.averageRpeChange = averageRpeChange;
        this.weeksInARow = weeksInARow;
        this.exercises = List.copyOf(exercises);
    }

    public Optional<BigDecimal> getAverageRpe() {
        return Optional.ofNullable(averageRpe);
    }

    public Optional<Integer> getSessionsDoneChange() {
        return Optional.ofNullable(sessionsDoneChange);
    }

    public Optional<BigDecimal> getAverageRpeChange() {
        return Optional.ofNullable(averageRpeChange);
    }

    /**
     * One exercise logged in the period: its top set in the period, whether its best set over all logs was logged
     * today, its top set and mean RPE in each period week it was logged in, and its 3 newest sessions over all logs,
     * newest first. The change is absent when it cannot be compared.
     */
    @Getter
    public static class ExerciseProgress {

        private final Exercise exercise;
        private final TopSet best;

        @Getter(AccessLevel.NONE)
        private final TopSetChange topSetChange;

        private final boolean prToday;
        private final List<Week> weeks;
        private final List<Recent> recent;

        /** {@code topSetChange} may be {@code null}. */
        public ExerciseProgress(
                Exercise exercise,
                TopSet best,
                TopSetChange topSetChange,
                boolean prToday,
                List<Week> weeks,
                List<Recent> recent) {
            this.exercise = Objects.requireNonNull(exercise);
            this.best = Objects.requireNonNull(best);
            this.topSetChange = topSetChange;
            this.prToday = prToday;
            this.weeks = List.copyOf(weeks);
            this.recent = List.copyOf(recent);
        }

        public Optional<TopSetChange> getTopSetChange() {
            return Optional.ofNullable(topSetChange);
        }

        /** The exercise's top set and mean RPE in one plan week. */
        public record Week(int number, TopSet top, BigDecimal averageRpe) {}

        /** The exercise's top set in one session log. */
        public record Recent(LocalDate date, TopSet top) {}
    }

    /** How a top set changed: in kilograms when both sets have a load, otherwise in repetitions or seconds. */
    public record TopSetChange(BigDecimal loadKg, Integer amount) {

        public TopSetChange {
            if ((loadKg == null) == (amount == null)) {
                throw new IllegalArgumentException("a change is in load or in amount");
            }
        }
    }
}
