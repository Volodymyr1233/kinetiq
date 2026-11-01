package com.kinetiq.exercise.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.output.ExerciseRepository;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExerciseListingService implements ListExercisesUseCase {

    private final ExerciseRepository exercises;

    // filtered in memory: the library is about 40 rows, and Polish collation is simpler here than in SQL
    @Override
    public ExerciseListing list(ListExercisesQuery query) {
        List<Exercise> library = exercises.findVisibleTo(query.trainer());
        Collator collator = Collator.getInstance(query.locale());
        collator.setStrength(Collator.PRIMARY);
        Map<ExerciseId, Integer> plansUsing = exercises.countPlansUsing(query.trainer());
        Comparator<Exercise> byName = Comparator.comparing(
                (Exercise exercise) -> exercise.getName().in(query.locale()), collator);
        List<ExerciseListing.Entry> entries = library.stream()
                .filter(exercise -> query.category() == null || exercise.getCategory() == query.category())
                .filter(exercise -> exercise.matches(query.name()))
                .sorted(byName.thenComparing(exercise -> exercise.getId().value()))
                .map(exercise -> new ExerciseListing.Entry(exercise, plansUsing.getOrDefault(exercise.getId(), 0)))
                .toList();
        return new ExerciseListing(entries, library.size());
    }

    @Override
    public List<Exercise> library(AccountId trainer) {
        return exercises.findVisibleTo(trainer);
    }
}
