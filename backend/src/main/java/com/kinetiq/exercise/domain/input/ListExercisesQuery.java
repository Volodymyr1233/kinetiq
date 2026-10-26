package com.kinetiq.exercise.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Category;
import java.util.Locale;
import java.util.Objects;

/**
 * Asks for the exercises of the trainer's library in a category, or in all when {@code category} is {@code null},
 * whose name contains {@code name}. The locale decides the order of the result.
 */
public record ListExercisesQuery(AccountId trainer, Category category, String name, Locale locale) {

    public ListExercisesQuery {
        Objects.requireNonNull(trainer);
        name = Objects.requireNonNullElse(name, "");
        Objects.requireNonNull(locale);
    }
}
