package com.kinetiq.exercise.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.exercise.domain.exception.ExerciseLimitReachedException;
import com.kinetiq.exercise.domain.exception.ExerciseNameTakenException;
import com.kinetiq.exercise.domain.exception.ExerciseNotFoundException;
import com.kinetiq.exercise.domain.input.CreateExerciseCommand;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.UpdateExerciseCommand;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExerciseManagementServiceTest {

    private static final AccountId TRAINER = AccountId.random();
    private static final AccountId OTHER_TRAINER = AccountId.random();

    private static final Exercise PLANK = new Exercise(
            ExerciseId.random(),
            null,
            new LocalizedText("Plank", "Deska"),
            Category.CORE,
            new LocalizedText("Bodyweight", "Masa ciała"),
            Measure.SECONDS,
            3,
            45,
            60,
            new LocalizedText("Ribs down.", "Żebra w dół."));

    private final InMemoryExerciseRepository repository = new InMemoryExerciseRepository(PLANK);
    private final ExerciseManagementService service = new ExerciseManagementService(repository);

    private static Exercise own(String name, AccountId owner) {
        return new Exercise(
                ExerciseId.random(),
                owner,
                new LocalizedText(name, ""),
                Category.LEGS,
                new LocalizedText("", ""),
                Measure.REPS,
                3,
                10,
                90,
                new LocalizedText("", ""));
    }

    private Exercise stored(Exercise exercise) {
        repository.byId.put(exercise.getId(), exercise);
        return exercise;
    }

    private static CreateExerciseCommand create(AccountId trainer, String name) {
        return new CreateExerciseCommand(
                trainer, name, Category.LEGS, Measure.REPS, " Dumbbell ", 3, 10, 90, "  Elbows inside the knees. ");
    }

    private static UpdateExerciseCommand update(AccountId trainer, ExerciseId id, String name) {
        return new UpdateExerciseCommand(trainer, id, name, Category.FULL_BODY, "Kettlebell", 4, 12, 60, "Chest up.");
    }

    @Nested
    class Create {

        @Test
        void shouldSaveWithTrainerAsOwnerAndTrimmedText_whenNameIsFree() {
            ExerciseListing.Entry entry = service.create(create(TRAINER, "  Goblet squat "));

            Exercise saved = repository.findById(entry.exercise().getId()).orElseThrow();
            assertThat(saved.isOwnedBy(TRAINER)).isTrue();
            assertThat(saved.getName()).isEqualTo(new LocalizedText("Goblet squat", ""));
            assertThat(saved.getEquipment()).isEqualTo(new LocalizedText("Dumbbell", ""));
            assertThat(saved.getCues()).isEqualTo(new LocalizedText("Elbows inside the knees.", ""));
            assertThat(saved.getMeasure()).isEqualTo(Measure.REPS);
            assertThat(entry.plansUsing()).isZero();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Plank", "deska", " PLANK ", " Plank ", "Goblet squat", "goblet SQUAT"})
        void shouldRefuseAndSaveNothing_whenNameIsInTheTrainersLibrary(String name) {
            stored(own("Goblet squat", TRAINER));
            Map<ExerciseId, Exercise> before = Map.copyOf(repository.byId);

            assertThatThrownBy(() -> service.create(create(TRAINER, name)))
                    .isInstanceOf(ExerciseNameTakenException.class);
            assertThat(repository.byId).isEqualTo(before);
        }

        @Test
        void shouldRefuseAndSaveNothing_whenNameIsOnlyNoBreakSpaces() {
            assertThatThrownBy(() -> service.create(create(TRAINER, "  ")))
                    .isInstanceOf(InvalidValueException.class);
            assertThat(repository.byId).containsOnlyKeys(PLANK.getId());
        }

        @Test
        void shouldSave_whenOnlyAnotherTrainerHasTheName() {
            stored(own("Goblet squat", OTHER_TRAINER));

            ExerciseListing.Entry entry = service.create(create(TRAINER, "Goblet squat"));

            assertThat(repository.findVisibleTo(TRAINER)).contains(entry.exercise());
        }

        @Test
        void shouldSaveThe200thAndRefuseThe201st_whenOnlyOwnExercisesCount() {
            for (int i = 1; i < Exercise.MAX_OWN_PER_TRAINER; i++) {
                stored(own("Own " + i, TRAINER));
                stored(own("Other " + i, OTHER_TRAINER));
            }

            service.create(create(TRAINER, "Own 200"));
            int saved = repository.byId.size();

            assertThatThrownBy(() -> service.create(create(TRAINER, "Own 201")))
                    .isInstanceOf(ExerciseLimitReachedException.class);
            assertThat(repository.byId).hasSize(saved);
        }

        @Test
        void shouldNeverLimitUpdates_whenTrainerOwnsTheMaximum() {
            Exercise first = stored(own("Own 0", TRAINER));
            for (int i = 1; i < Exercise.MAX_OWN_PER_TRAINER; i++) {
                stored(own("Own " + i, TRAINER));
            }

            service.update(update(TRAINER, first.getId(), "Own renamed"));

            assertThat(repository.findById(first.getId()).orElseThrow().getName().en()).isEqualTo("Own renamed");
        }
    }

    @Nested
    class Update {

        @Test
        void shouldReplaceValuesAndKeepMeasure_whenExerciseIsOwn() {
            Exercise exercise = stored(own("Goblet squat", TRAINER));

            service.update(update(TRAINER, exercise.getId(), " Goblet squat hold "));

            Exercise saved = repository.findById(exercise.getId()).orElseThrow();
            assertThat(saved.getName()).isEqualTo(new LocalizedText("Goblet squat hold", ""));
            assertThat(saved.getCategory()).isEqualTo(Category.FULL_BODY);
            assertThat(saved.getEquipment().en()).isEqualTo("Kettlebell");
            assertThat(saved.getDefaultSets()).isEqualTo(4);
            assertThat(saved.getDefaultAmount()).isEqualTo(12);
            assertThat(saved.getRestSeconds()).isEqualTo(60);
            assertThat(saved.getCues().en()).isEqualTo("Chest up.");
            assertThat(saved.getMeasure()).isEqualTo(Measure.REPS);
            assertThat(saved.isOwnedBy(TRAINER)).isTrue();
        }

        @Test
        void shouldNotClashWithItself_whenNameIsUnchanged() {
            Exercise exercise = stored(own("Goblet squat", TRAINER));

            service.update(update(TRAINER, exercise.getId(), "GOBLET squat"));

            assertThat(repository.findById(exercise.getId()).orElseThrow().getName().en()).isEqualTo("GOBLET squat");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Deska", "wall sit"})
        void shouldRefuseAndChangeNothing_whenNameBelongsToAnotherVisibleExercise(String name) {
            Exercise exercise = stored(own("Goblet squat", TRAINER));
            stored(own("Wall sit", TRAINER));

            assertThatThrownBy(() -> service.update(update(TRAINER, exercise.getId(), name)))
                    .isInstanceOf(ExerciseNameTakenException.class);
            assertThat(repository.findById(exercise.getId())).contains(exercise);
        }

        @Test
        void shouldRefuseAsNotFound_whenExerciseIsStarter() {
            assertThatThrownBy(() -> service.update(update(TRAINER, PLANK.getId(), "Plank")))
                    .isInstanceOf(ExerciseNotFoundException.class);
            assertThat(repository.findById(PLANK.getId())).contains(PLANK);
        }

        @Test
        void shouldRefuseAsNotFound_whenExerciseIsAnotherTrainers() {
            Exercise foreign = stored(own("Goblet squat", OTHER_TRAINER));

            assertThatThrownBy(() -> service.update(update(TRAINER, foreign.getId(), "Taken over")))
                    .isInstanceOf(ExerciseNotFoundException.class);
            assertThat(repository.findById(foreign.getId())).contains(foreign);
        }

        @Test
        void shouldRefuseAsNotFound_whenExerciseIsUnknown() {
            Map<ExerciseId, Exercise> before = Map.copyOf(repository.byId);

            assertThatThrownBy(() -> service.update(update(TRAINER, ExerciseId.random(), "Anything")))
                    .isInstanceOf(ExerciseNotFoundException.class);
            assertThat(repository.byId).isEqualTo(before);
        }
    }
}
