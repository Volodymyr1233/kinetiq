package com.kinetiq.planchange.domain.rule;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.LoggedSet;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Fires when the mean RPE of the logged sets is above 8 in every week of the window. */
public final class HighRpeRule implements PlanChangeRule {

    private static final int THRESHOLD = 8;

    @Override
    public Rule rule() {
        return Rule.HIGH_RPE;
    }

    @Override
    public Optional<Finding> evaluate(List<WeekTraining> window) {
        if (!window.stream().allMatch(week -> meanAbove(week.sets()))) {
            return Optional.empty();
        }
        Set<ExerciseId> exercises = window.stream()
                .flatMap(week -> week.sets().stream())
                .collect(Collectors.groupingBy(LoggedSet::exercise))
                .entrySet()
                .stream()
                .filter(entry -> meanAbove(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return Optional.of(new Finding(Rule.HIGH_RPE, exercises));
    }

    // the sum against the count, not a rounded mean: 8.04 must read as above 8
    private static boolean meanAbove(List<LoggedSet> sets) {
        return !sets.isEmpty() && sets.stream().mapToLong(LoggedSet::rpe).sum() > (long) THRESHOLD * sets.size();
    }
}
