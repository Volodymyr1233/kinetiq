package com.kinetiq.exercise.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import java.util.List;

/** The exercises that matched a query, and {@code total}, the size of the whole library. */
public record ExerciseListing(List<Entry> entries, int total) {

    public ExerciseListing {
        entries = List.copyOf(entries);
    }

    /** An exercise and the number of plans that use it. */
    public record Entry(Exercise exercise, int plansUsing) {}
}
