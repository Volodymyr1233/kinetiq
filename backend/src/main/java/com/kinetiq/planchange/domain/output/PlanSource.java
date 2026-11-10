package com.kinetiq.planchange.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Read access to the plans and exercise names that plan changes are made from and shown with. */
public interface PlanSource {

    /** The client's published plan; empty for a draft or no plan. */
    Optional<PlanSnapshot> currentPlan(AccountId client);

    /** The library name of each exercise in {@code language}, English where it has no other, and its measure. */
    Map<ExerciseId, ExerciseLabel> exercises(Set<ExerciseId> exercises, Language language);
}
