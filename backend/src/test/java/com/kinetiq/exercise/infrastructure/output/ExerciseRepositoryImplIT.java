package com.kinetiq.exercise.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.exception.ExerciseNameTakenException;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// starter rows go in through Spring Data, as only migrations store them; own exercises go in through the adapter
@DataJpaTest
@Import({TestcontainersConfiguration.class, ExerciseRepositoryImpl.class, ExerciseMapperImpl.class})
class ExerciseRepositoryImplIT {

    @Autowired
    private ExerciseRepositoryImpl repository;

    @Autowired
    private JpaExerciseRepository jpa;

    @Autowired
    private JdbcTemplate jdbc;

    private static ExerciseEntity entity(String name, String namePl) {
        ExerciseEntity entity = new ExerciseEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setNamePl(namePl);
        entity.setCategory(Category.CORE);
        entity.setEquipment("Bodyweight");
        entity.setEquipmentPl(namePl == null ? null : "Masa ciała");
        entity.setMeasure(Measure.SECONDS);
        entity.setDefaultSets(3);
        entity.setDefaultAmount(30);
        entity.setRestSeconds(0);
        entity.setCues("Squeeze glutes.");
        entity.setCuesPl(namePl == null ? null : "Napnij pośladki.");
        return entity;
    }

    private UUID insertAccount() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, time_zone, email_confirmed,
                                      created_at, updated_at, version)
                values (?, 'Marek Nowak', ?, 'hash', 'TRAINER', 'Europe/Warsaw', true, now(), now(), 0)
                """,
                id,
                id + "@example.com");
        return id;
    }

    // migrations may already have stored starter exercises, so tests look for their own rows
    private Exercise findVisible(UUID id) {
        return repository.findVisibleTo(AccountId.random()).stream()
                .filter(exercise -> exercise.getId().value().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static Exercise own(UUID owner, String name) {
        return new Exercise(
                ExerciseId.random(),
                new AccountId(owner),
                new LocalizedText(name, ""),
                Category.LEGS,
                new LocalizedText("Dumbbell", ""),
                Measure.REPS,
                3,
                10,
                90,
                new LocalizedText("", ""));
    }

    @Test
    void shouldReturnStarterAndOwnOnly_whenFindingVisibleExercises() {
        UUID trainer = insertAccount();
        UUID otherTrainer = insertAccount();
        ExerciseEntity starter = jpa.saveAndFlush(entity("Plank", "Deska"));
        Exercise mine = own(trainer, "Goblet squat");
        Exercise theirs = own(otherTrainer, "Wall sit");
        repository.save(mine);
        repository.save(theirs);

        assertThat(repository.findVisibleTo(new AccountId(trainer)))
                .extracting(Exercise::getId)
                .contains(new ExerciseId(starter.getId()), mine.getId())
                .doesNotContain(theirs.getId());
    }

    @Test
    void shouldRoundTripEveryFieldWithNoPolish_whenSavingNewExercise() {
        UUID trainer = insertAccount();
        Exercise exercise = new Exercise(
                ExerciseId.random(),
                new AccountId(trainer),
                new LocalizedText("Copenhagen plank", ""),
                Category.CORE,
                new LocalizedText("Bench", ""),
                Measure.SECONDS,
                2,
                30,
                45,
                new LocalizedText("Hips high.", ""));

        repository.save(exercise);

        ExerciseEntity row = jpa.findById(exercise.getId().value()).orElseThrow();
        assertThat(row.getOwnerId()).isEqualTo(trainer);
        assertThat(row.getNamePl()).isNull();
        assertThat(row.getEquipmentPl()).isNull();
        assertThat(row.getCuesPl()).isNull();
        Exercise read = repository.findById(exercise.getId()).orElseThrow();
        assertThat(read)
                .usingRecursiveComparison()
                .isEqualTo(exercise);
    }

    @Test
    void shouldUpdateSameRowKeepingCreatedAt_whenSavingEditedExercise() {
        Exercise exercise = own(insertAccount(), "Goblet squat");
        repository.save(exercise);
        ExerciseEntity before = jpa.findById(exercise.getId().value()).orElseThrow();
        Instant createdAt = before.getCreatedAt();
        long version = before.getVersion();

        repository.save(exercise.edit(
                new LocalizedText("Goblet squat hold", ""),
                Category.FULL_BODY,
                new LocalizedText("", ""),
                4,
                12,
                60,
                new LocalizedText("Chest up.", "")));

        ExerciseEntity after = jpa.findById(exercise.getId().value()).orElseThrow();
        assertThat(after.getName()).isEqualTo("Goblet squat hold");
        assertThat(after.getCategory()).isEqualTo(Category.FULL_BODY);
        assertThat(after.getCreatedAt()).isEqualTo(createdAt);
        assertThat(after.getVersion()).isGreaterThan(version);
    }

    @Test
    void shouldRefuseAsNameTaken_whenOwnNamesDifferOnlyInLetterCase() {
        UUID trainer = insertAccount();
        repository.save(own(trainer, "Goblet squat"));

        assertThatThrownBy(() -> repository.save(own(trainer, "GOBLET SQUAT")))
                .isInstanceOf(ExerciseNameTakenException.class);
    }

    @Test
    void shouldAllowSameName_whenTwoTrainersOwnIt() {
        repository.save(own(insertAccount(), "Goblet squat"));
        Exercise other = own(insertAccount(), "Goblet squat");

        repository.save(other);

        assertThat(jpa.findById(other.getId().value())).isPresent();
    }

    // the service refuses starter names; the index only covers simultaneous creates of own exercises
    @Test
    void shouldNotRefuseInIndex_whenOwnNameEqualsStarterName() {
        jpa.saveAndFlush(entity("Plank", "Deska"));
        Exercise plank = own(insertAccount(), "Plank");

        repository.save(plank);

        assertThat(jpa.findById(plank.getId().value())).isPresent();
    }

    @Test
    void shouldReadBackBothLanguages_whenPolishTextsAreStored() {
        ExerciseEntity plank = jpa.saveAndFlush(entity("Plank", "Deska"));

        Exercise exercise = findVisible(plank.getId());

        assertThat(exercise.getName()).isEqualTo(new LocalizedText("Plank", "Deska"));
        assertThat(exercise.getEquipment()).isEqualTo(new LocalizedText("Bodyweight", "Masa ciała"));
        assertThat(exercise.getCues()).isEqualTo(new LocalizedText("Squeeze glutes.", "Napnij pośladki."));
        assertThat(exercise.getCategory()).isEqualTo(Category.CORE);
        assertThat(exercise.getMeasure()).isEqualTo(Measure.SECONDS);
        assertThat(exercise.getDefaultSets()).isEqualTo(3);
        assertThat(exercise.getDefaultAmount()).isEqualTo(30);
        assertThat(exercise.getRestSeconds()).isZero();
        assertThat(exercise.getOwner()).isEmpty();
    }

    @Test
    void shouldReadEmptyPolish_whenNoPolishTextsAreStored() {
        ExerciseEntity plank = jpa.saveAndFlush(entity("Plank", null));

        Exercise exercise = findVisible(plank.getId());

        assertThat(exercise.getName()).isEqualTo(new LocalizedText("Plank", ""));
        assertThat(exercise.getEquipment().pl()).isEmpty();
        assertThat(exercise.getCues().pl()).isEmpty();
    }

    @Test
    void shouldRefuse_whenDefaultSetsAre0() {
        ExerciseEntity entity = entity("Plank", null);
        entity.setDefaultSets(0);

        assertThatThrownBy(() -> jpa.saveAndFlush(entity)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRefuse_whenDefaultAmountIs0() {
        ExerciseEntity entity = entity("Plank", null);
        entity.setDefaultAmount(0);

        assertThatThrownBy(() -> jpa.saveAndFlush(entity)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRefuse_whenNameIsBlank() {
        assertThatThrownBy(() -> jpa.saveAndFlush(entity("  ", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRefuse_whenCategoryIsUnknown() {
        assertThatThrownBy(() -> jdbc.update(
                        """
                        insert into exercises (id, name, category, equipment, measure, default_sets, default_amount,
                                               rest_seconds, cues, created_at, updated_at, version)
                        values (?, 'Plank', 'NECK', 'Bodyweight', 'SECONDS', 3, 30, 0, '', now(), now(), 0)
                        """,
                        UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
