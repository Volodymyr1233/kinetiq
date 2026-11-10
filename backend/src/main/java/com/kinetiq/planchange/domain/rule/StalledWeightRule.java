package com.kinetiq.planchange.domain.rule;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.LoggedSet;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fires for the exercises with a load that were logged in every week of the window, whose best load rose by less than 5%
 * and whose mean RPE is at most 8: the client is comfortable but no longer progressing.
 */
public final class StalledWeightRule implements PlanChangeRule {

    private static final BigDecimal REQUIRED_RISE = new BigDecimal("1.05");
    private static final int RPE_LIMIT = 8;

    @Override
    public Rule rule() {
        return Rule.STALLED_WEIGHT;
    }

    @Override
    public Optional<Finding> evaluate(List<WeekTraining> window) {
        Set<ExerciseId> stalled = window.getFirst().sets().stream()
                .filter(set -> set.loadKg() != null)
                .map(LoggedSet::exercise)
                .distinct()
                .filter(exercise -> stalled(exercise, window))
                .collect(Collectors.toSet());
        if (stalled.isEmpty()) {
            return Optional.empty();
        }
        List<Figure> figures = stalled.stream()
                .sorted(Comparator.comparing(ExerciseId::value))
                .flatMap(exercise -> window.stream()
                        .map(week -> Figure.bestLoad(
                                exercise, week.monday(), heaviest(exercise, week).orElseThrow())))
                .toList();
        return Optional.of(new Finding(Rule.STALLED_WEIGHT, stalled, figures));
    }

    private static boolean stalled(ExerciseId exercise, List<WeekTraining> window) {
        List<BigDecimal> best = new ArrayList<>();
        long rpeSum = 0;
        long count = 0;
        for (WeekTraining week : window) {
            Optional<BigDecimal> heaviest = heaviest(exercise, week);
            if (heaviest.isEmpty()) {
                return false;
            }
            best.add(heaviest.get());
            List<LoggedSet> sets = setsOf(exercise, week);
            rpeSum += sets.stream().mapToLong(LoggedSet::rpe).sum();
            count += sets.size();
        }
        // exact decimals and sums: a rounded mean or a double would misjudge exactly 5% and exactly 8
        boolean belowRise = best.getLast().compareTo(best.getFirst().multiply(REQUIRED_RISE)) < 0;
        return belowRise && rpeSum <= (long) RPE_LIMIT * count;
    }

    private static Optional<BigDecimal> heaviest(ExerciseId exercise, WeekTraining week) {
        return setsOf(exercise, week).stream()
                .map(LoggedSet::loadKg)
                .filter(load -> load != null)
                .max(BigDecimal::compareTo);
    }

    private static List<LoggedSet> setsOf(ExerciseId exercise, WeekTraining week) {
        return week.sets().stream().filter(set -> set.exercise().equals(exercise)).toList();
    }
}
