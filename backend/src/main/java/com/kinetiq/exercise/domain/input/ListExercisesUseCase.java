package com.kinetiq.exercise.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Exercise;
import java.util.List;

public interface ListExercisesUseCase {

    ExerciseListing list(ListExercisesQuery query);

    /** The trainer's whole library in no particular order: the starter exercises and their own, nobody else's. */
    List<Exercise> library(AccountId trainer);
}
