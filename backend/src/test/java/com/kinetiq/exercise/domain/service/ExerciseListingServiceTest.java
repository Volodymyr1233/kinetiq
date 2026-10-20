package com.kinetiq.exercise.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.exercise.domain.output.ExerciseRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExerciseListingServiceTest {

    private static final Locale POLISH = Locale.forLanguageTag("pl");

    private static Exercise exercise(String id, String english, String polish, Category category) {
        return new Exercise(
                new ExerciseId(UUID.fromString("00000000-0000-4000-9100-0000000000" + id)),
                null,
                new LocalizedText(english, polish),
                category,
                new LocalizedText("Bodyweight", "Masa ciała"),
                Measure.REPS,
                3,
                10,
                60,
                new LocalizedText("", ""));
    }

    private static final Exercise SQUAT = exercise("01", "Back squat", "Przysiad ze sztangą", Category.LEGS);
    private static final Exercise SPLIT_SQUAT = exercise("02", "Bulgarian split squat", "Przysiad bułgarski", Category.LEGS);
    private static final Exercise BENCH = exercise("03", "Bench press", "Wyciskanie sztangi leżąc", Category.CHEST);
    private static final Exercise PLANK = exercise("04", "Plank", "Deska", Category.CORE);

    private static ExerciseListing list(List<Exercise> library, Category category, String name, Locale locale) {
        ExerciseRepository repository = () -> library;
        return new ExerciseListingService(repository).list(new ListExercisesQuery(category, name, locale));
    }

    private static ExerciseListing list(Category category, String name) {
        return list(List.of(SQUAT, SPLIT_SQUAT, BENCH, PLANK), category, name, Locale.ENGLISH);
    }

    private static List<String> englishNames(ExerciseListing listing) {
        return listing.entries().stream()
                .map(entry -> entry.exercise().getName().en())
                .toList();
    }

    @Test
    void shouldListWholeLibraryByName_whenThereIsNoFilter() {
        ExerciseListing listing = list(null, null);

        assertThat(englishNames(listing)).containsExactly("Back squat", "Bench press", "Bulgarian split squat", "Plank");
        assertThat(listing.total()).isEqualTo(4);
    }

    @Test
    void shouldKeepOnlyTheCategory_whenCategoryIsGiven() {
        assertThat(englishNames(list(Category.LEGS, ""))).containsExactly("Back squat", "Bulgarian split squat");
    }

    @Test
    void shouldFindByEnglishName_whenNameIsGiven() {
        assertThat(englishNames(list(null, " SQUAT "))).containsExactly("Back squat", "Bulgarian split squat");
    }

    @Test
    void shouldFindByPolishName_whenNameIsGiven() {
        assertThat(englishNames(list(null, "sztang"))).containsExactly("Back squat", "Bench press");
    }

    @Test
    void shouldApplyBothFilters_whenCategoryAndNameAreGiven() {
        assertThat(englishNames(list(Category.CHEST, "sztang"))).containsExactly("Bench press");
    }

    @Test
    void shouldCountWholeLibrary_whenFiltersLeaveFewerExercises() {
        ExerciseListing listing = list(Category.CORE, "nothing like this");

        assertThat(listing.entries()).isEmpty();
        assertThat(listing.total()).isEqualTo(4);
    }

    @Test
    void shouldOrderByPolishAlphabet_whenLocaleIsPolish() {
        List<Exercise> library = List.of(
                exercise("01", "Bridge", "Mostek", Category.CORE),
                exercise("02", "Calf raise", "Łydki na stopniu", Category.LEGS),
                exercise("03", "Rope climb", "Lina", Category.BACK));

        assertThat(list(library, null, "", POLISH).entries())
                .extracting(entry -> entry.exercise().getName().pl())
                .containsExactly("Lina", "Łydki na stopniu", "Mostek");
        assertThat(englishNames(list(library, null, "", Locale.ENGLISH)))
                .containsExactly("Bridge", "Calf raise", "Rope climb");
    }

    @Test
    void shouldOrderByIdAndIgnoreCase_whenNamesAreEqual() {
        List<Exercise> library = List.of(
                exercise("02", "plank", "", Category.CORE),
                exercise("03", "Bench press", "", Category.CHEST),
                exercise("01", "Plank", "", Category.CORE));

        assertThat(list(library, null, "", Locale.ENGLISH).entries())
                .extracting(entry -> entry.exercise().getId().value().toString())
                .containsExactly(
                        "00000000-0000-4000-9100-000000000003",
                        "00000000-0000-4000-9100-000000000001",
                        "00000000-0000-4000-9100-000000000002");
    }

    @Test
    void shouldReportNoPlans_whenPlansDoNotExistYet() {
        assertThat(list(null, "").entries()).extracting(ExerciseListing.Entry::plansUsing).containsOnly(0);
    }
}
