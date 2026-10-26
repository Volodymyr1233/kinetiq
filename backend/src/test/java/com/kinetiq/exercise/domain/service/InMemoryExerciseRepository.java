package com.kinetiq.exercise.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.exception.ExerciseNameTakenException;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.output.ExerciseRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

class InMemoryExerciseRepository implements ExerciseRepository {

    final Map<ExerciseId, Exercise> byId = new LinkedHashMap<>();

    InMemoryExerciseRepository(Exercise... exercises) {
        for (Exercise exercise : exercises) {
            byId.put(exercise.getId(), exercise);
        }
    }

    @Override
    public List<Exercise> findVisibleTo(AccountId trainer) {
        return byId.values().stream()
                .filter(exercise -> exercise.isStarter() || exercise.isOwnedBy(trainer))
                .toList();
    }

    @Override
    public Optional<Exercise> findById(ExerciseId id) {
        return Optional.ofNullable(byId.get(id));
    }

    // honours the unique index on an owner's names, as the database does
    @Override
    public void save(Exercise exercise) {
        boolean clash = byId.values().stream()
                .anyMatch(other -> !other.getId().equals(exercise.getId())
                        && !other.isStarter()
                        && other.getOwner().equals(exercise.getOwner())
                        && lower(other).equals(lower(exercise)));
        if (clash) {
            throw new ExerciseNameTakenException();
        }
        byId.put(exercise.getId(), exercise);
    }

    private static String lower(Exercise exercise) {
        return exercise.getName().en().toLowerCase(Locale.ROOT);
    }
}
