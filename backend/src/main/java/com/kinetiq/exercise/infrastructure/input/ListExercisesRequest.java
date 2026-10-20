package com.kinetiq.exercise.infrastructure.input;

import com.kinetiq.exercise.domain.model.Category;
import jakarta.validation.constraints.Size;

/** Query parameters of the library listing; both are optional. */
record ListExercisesRequest(Category category, @Size(max = 100) String name) {}
