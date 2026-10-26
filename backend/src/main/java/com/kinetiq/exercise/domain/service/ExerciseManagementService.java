package com.kinetiq.exercise.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.exception.ExerciseLimitReachedException;
import com.kinetiq.exercise.domain.exception.ExerciseNameTakenException;
import com.kinetiq.exercise.domain.exception.ExerciseNotFoundException;
import com.kinetiq.exercise.domain.input.CreateExerciseCommand;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ManageExercisesUseCase;
import com.kinetiq.exercise.domain.input.UpdateExerciseCommand;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.output.ExerciseRepository;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExerciseManagementService implements ManageExercisesUseCase {

    private static final Pattern EDGE_SPACES = Pattern.compile("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$");

    private final ExerciseRepository exercises;

    @Override
    public ExerciseListing.Entry create(CreateExerciseCommand command) {
        List<Exercise> visible = exercises.findVisibleTo(command.trainer());
        // ponytail: simultaneous creates can overshoot the cap by a few; count in SQL under a lock if it matters
        if (visible.stream().filter(exercise -> exercise.isOwnedBy(command.trainer())).count()
                >= Exercise.MAX_OWN_PER_TRAINER) {
            throw new ExerciseLimitReachedException();
        }
        requireFreeName(visible, command.name(), null);
        Exercise exercise = new Exercise(
                ExerciseId.random(),
                command.trainer(),
                text(command.name()),
                command.category(),
                text(command.equipment()),
                command.measure(),
                command.sets(),
                command.amount(),
                command.restSeconds(),
                text(command.cues()));
        exercises.save(exercise);
        return listed(exercise);
    }

    @Override
    public ExerciseListing.Entry update(UpdateExerciseCommand command) {
        Exercise exercise = exercises.findById(command.id())
                .filter(found -> found.isOwnedBy(command.trainer()))
                .orElseThrow(ExerciseNotFoundException::new);
        requireFreeName(exercises.findVisibleTo(command.trainer()), command.name(), exercise.getId());
        Exercise edited = exercise.edit(
                text(command.name()),
                command.category(),
                text(command.equipment()),
                command.sets(),
                command.amount(),
                command.restSeconds(),
                text(command.cues()));
        exercises.save(edited);
        return listed(edited);
    }

    // ponytail: Postgres lower() and Java case folding can differ for rare characters; then only this check, not the
    // unique index, catches the clash
    private static void requireFreeName(List<Exercise> visible, String name, ExerciseId ownId) {
        String wanted = folded(name);
        boolean taken = !wanted.isEmpty() && visible.stream()
                .filter(exercise -> !exercise.getId().equals(ownId))
                .anyMatch(exercise -> folded(exercise.getName().en()).equals(wanted)
                        || folded(exercise.getName().pl()).equals(wanted));
        if (taken) {
            throw new ExerciseNameTakenException();
        }
    }

    private static String folded(String name) {
        return trimmed(name).toLowerCase(Locale.ROOT);
    }

    // an own exercise keeps the text as typed, in whatever language, with no Polish text of its own
    private static LocalizedText text(String typed) {
        return new LocalizedText(trimmed(typed), "");
    }

    // also trims no-break and other Unicode spaces, which strip() keeps, as the dialog's trim() does
    private static String trimmed(String text) {
        return EDGE_SPACES.matcher(text).replaceAll("");
    }

    // plans arrive with story #68, which counts them
    private static ExerciseListing.Entry listed(Exercise exercise) {
        return new ExerciseListing.Entry(exercise, 0);
    }
}
