package com.kinetiq.exercise.domain.input;

public interface ManageExercisesUseCase {

    /**
     * Adds an exercise owned by the trainer and returns it as the listing shows it. Refused when another exercise the
     * trainer sees has the name, ignoring letter case, or when the trainer already owns the maximum number.
     */
    ExerciseListing.Entry create(CreateExerciseCommand command);

    /**
     * Replaces the values of the trainer's own exercise, keeping its measure. A starter exercise, another trainer's and
     * an unknown one are all refused as not found; a name clash is refused like in {@link #create}.
     */
    ExerciseListing.Entry update(UpdateExerciseCommand command);
}
