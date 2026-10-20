package com.kinetiq.exercise.domain.input;

public interface ListExercisesUseCase {

    ExerciseListing list(ListExercisesQuery query);
}
