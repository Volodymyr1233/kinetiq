package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.output.PlanSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Plans per client, and exercise names that carry the language they were asked in. */
class FakePlanSource implements PlanSource {

    final Map<AccountId, PlanSnapshot> plans = new HashMap<>();
    final Map<ExerciseId, String> names = new HashMap<>();

    @Override
    public Optional<PlanSnapshot> currentPlan(AccountId client) {
        return Optional.ofNullable(plans.get(client));
    }

    @Override
    public Map<ExerciseId, ExerciseLabel> exercises(Set<ExerciseId> exercises, Language language) {
        return exercises.stream()
                .filter(names::containsKey)
                .collect(Collectors.toMap(
                        exercise -> exercise,
                        exercise -> new ExerciseLabel(
                                language == Language.PL ? names.get(exercise) + " PL" : names.get(exercise),
                                Measure.REPS)));
    }
}
