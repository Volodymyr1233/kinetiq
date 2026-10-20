package com.kinetiq.exercise.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// the port has no save yet, so rows go in through Spring Data and come out through the adapter
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
    private Exercise findStarter(UUID id) {
        return repository.findStarter().stream()
                .filter(exercise -> exercise.getId().value().equals(id))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void shouldReadBackBothLanguages_whenPolishTextsAreStored() {
        ExerciseEntity plank = jpa.saveAndFlush(entity("Plank", "Deska"));

        Exercise exercise = findStarter(plank.getId());

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

        Exercise exercise = findStarter(plank.getId());

        assertThat(exercise.getName()).isEqualTo(new LocalizedText("Plank", ""));
        assertThat(exercise.getEquipment().pl()).isEmpty();
        assertThat(exercise.getCues().pl()).isEmpty();
    }

    @Test
    void shouldSkipOwnedExercise_whenFindingStarter() {
        ExerciseEntity starter = jpa.save(entity("Plank", "Deska"));
        ExerciseEntity owned = entity("Copenhagen plank", null);
        owned.setOwnerId(insertAccount());
        jpa.saveAndFlush(owned);

        assertThat(repository.findStarter())
                .extracting(exercise -> exercise.getId().value())
                .contains(starter.getId())
                .doesNotContain(owned.getId());
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
