package com.kinetiq.exercise.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ExerciseRepositoryImpl.class, ExerciseMapperImpl.class})
class StarterExerciseSetIT {

    @Autowired
    private ExerciseRepositoryImpl repository;

    private List<Exercise> starter;

    @BeforeEach
    void loadStarter() {
        starter = repository.findStarter();
    }

    @Test
    void shouldHold40ExercisesWithDistinctIds_whenMigrated() {
        assertThat(starter).hasSize(40);
        assertThat(starter).extracting(Exercise::getId).doesNotHaveDuplicates();
    }

    @Test
    void shouldHoldAtLeast3ExercisesPerCategory_whenMigrated() {
        for (Category category : Category.values()) {
            assertThat(starter)
                    .as(category.name())
                    .filteredOn(exercise -> exercise.getCategory() == category)
                    .hasSizeGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void shouldHoldAtLeast3TimedExercisesIncludingPlank_whenMigrated() {
        List<Exercise> timed = starter.stream()
                .filter(exercise -> exercise.getMeasure() == Measure.SECONDS)
                .toList();

        assertThat(timed).hasSizeGreaterThanOrEqualTo(3);
        assertThat(timed).extracting(exercise -> exercise.getName().en()).contains("Plank");
    }

    @Test
    void shouldHavePolishNameEquipmentAndCues_whenMigrated() {
        assertThat(starter)
                .flatExtracting(exercise -> Stream.of(exercise.getName(), exercise.getEquipment(), exercise.getCues())
                        .map(LocalizedText::pl)
                        .toList())
                .allSatisfy(polish -> assertThat(polish).isNotBlank());
    }

    @Test
    void shouldHaveTranslatedNames_whenMigrated() {
        assertThat(starter)
                .filteredOn(exercise -> !exercise.getName().pl().equals(exercise.getName().en()))
                .hasSizeGreaterThanOrEqualTo(30);
    }
}
