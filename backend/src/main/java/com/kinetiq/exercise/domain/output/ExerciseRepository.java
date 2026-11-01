package com.kinetiq.exercise.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExerciseRepository {

    /** Returns the starter exercises and the trainer's own, never another trainer's. */
    List<Exercise> findVisibleTo(AccountId trainer);

    /**
     * Counts, per exercise, the plans of the trainer's own clients that use it. Exercises in none of them are absent,
     * so another trainer's use of a starter exercise never shows.
     */
    Map<ExerciseId, Integer> countPlansUsing(AccountId trainer);

    /** Finds an exercise whoever owns it; the caller decides whether the trainer may see it. */
    Optional<Exercise> findById(ExerciseId id);

    /**
     * Inserts or updates the exercise. Throws {@code ExerciseNameTakenException} when two own exercises of one trainer
     * would share a name, ignoring letter case.
     */
    void save(Exercise exercise);
}
