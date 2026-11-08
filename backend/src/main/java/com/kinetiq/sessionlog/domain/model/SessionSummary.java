package com.kinetiq.sessionlog.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.sessionlog.domain.exception.NotFinishedException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * How a finished session went, as its sets are now. {@code volumeKg} counts only sets with a load in an exercise
 * measured in repetitions, and is empty when none does; {@code minutes} runs from the start to the finish, at least 1.
 */
public record SessionSummary(
        int setsLogged, int setsPlanned, Optional<BigDecimal> volumeKg, BigDecimal averageRpe, int minutes) {

    private static final int DECIMALS = 1;

    /** Works out the summary of a finished log; an unfinished one is {@code NotFinishedException}. */
    public static SessionSummary of(SessionLog log, Map<ExerciseId, Measure> measures) {
        int minutes = log.minutes().orElseThrow(NotFinishedException::new);
        var sets = log.getSetLogs();
        BigDecimal volume = sets.stream()
                .filter(set -> set.values().loadKg() != null && isRepetitions(log, set, measures))
                .map(set -> set.values().loadKg().multiply(BigDecimal.valueOf(set.values().amount())))
                .reduce(BigDecimal::add)
                .orElse(null);
        return new SessionSummary(
                sets.size(),
                log.setsPlanned(),
                Optional.ofNullable(volume).map(v -> v.setScale(DECIMALS, RoundingMode.HALF_UP)),
                log.averageRpe().orElse(BigDecimal.ZERO.setScale(DECIMALS)),
                minutes);
    }

    private static boolean isRepetitions(SessionLog log, SetLog set, Map<ExerciseId, Measure> measures) {
        return measures.get(log.getEntries().get(set.entryPosition()).exercise()) == Measure.REPS;
    }
}
