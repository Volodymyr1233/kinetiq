package com.kinetiq.exercise.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(ExerciseController.class)
@Import({SecurityConfig.class, WebConfig.class, ExerciseWebMapperImpl.class})
class ExerciseControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-9100-000000000026");

    private static final Exercise PLANK = new Exercise(
            new ExerciseId(ID),
            null,
            new LocalizedText("Plank", "Deska"),
            Category.CORE,
            new LocalizedText("Bodyweight", ""),
            Measure.SECONDS,
            3,
            45,
            60,
            new LocalizedText("Ribs down.", "Żebra w dół."));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListExercisesUseCase listExercises;

    @BeforeEach
    void stubListing() {
        when(listExercises.list(any())).thenReturn(new ExerciseListing(List.of(new ExerciseListing.Entry(PLANK, 0)), 40));
    }

    private MvcTestResult get(String query, String language) {
        return mvc.get().uri("/api/v1/exercises" + query).header("Accept-Language", language).exchange();
    }

    private ListExercisesQuery passedQuery() {
        ArgumentCaptor<ListExercisesQuery> query = ArgumentCaptor.forClass(ListExercisesQuery.class);
        verify(listExercises).list(query.capture());
        return query.getValue();
    }

    @Test
    @WithMockUser
    void shouldPassFiltersToUseCase_whenTheyAreGiven() {
        assertThat(get("?category=CORE&name=pla", "en")).hasStatusOk();

        assertThat(passedQuery()).isEqualTo(new ListExercisesQuery(Category.CORE, "pla", Locale.ENGLISH));
    }

    @Test
    @WithMockUser
    void shouldPassNoFilters_whenParametersAreAbsent() {
        assertThat(get("", "en")).hasStatusOk();

        assertThat(passedQuery()).isEqualTo(new ListExercisesQuery(null, "", Locale.ENGLISH));
    }

    @Test
    @WithMockUser
    void shouldAnswerInPolishWithEnglishFallback_whenPolishIsAsked() {
        var result = get("", "pl-PL,pl;q=0.9");

        assertThat(passedQuery().locale()).isEqualTo(Locale.forLanguageTag("pl"));
        assertThat(result).bodyJson().extractingPath("$.exercises[0].name").isEqualTo("Deska");
        assertThat(result).bodyJson().extractingPath("$.exercises[0].cues").isEqualTo("Żebra w dół.");
        assertThat(result).bodyJson().extractingPath("$.exercises[0].equipment").isEqualTo("Bodyweight");
    }

    @Test
    @WithMockUser
    void shouldAnswerInEnglish_whenLanguageIsUnsupported() {
        var result = get("", "de-DE");

        assertThat(passedQuery().locale()).isEqualTo(Locale.ENGLISH);
        assertThat(result).bodyJson().extractingPath("$.exercises[0].name").isEqualTo("Plank");
    }

    @Test
    @WithMockUser
    void shouldReturnExerciseTotalAndPlans_whenListed() {
        var result = get("", "en");

        assertThat(result).bodyJson().extractingPath("$.total").isEqualTo(40);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.exercises[0]")
                .isEqualTo(Map.of(
                        "id", ID.toString(),
                        "name", "Plank",
                        "category", "CORE",
                        "equipment", "Bodyweight",
                        "measure", "SECONDS",
                        "sets", 3,
                        "amount", 45,
                        "restSeconds", 60,
                        "cues", "Ribs down.",
                        "plansUsing", 0));
    }

    @Test
    @WithMockUser
    void shouldRefuseWithFieldError_whenCategoryIsUnknown() {
        var result = get("?category=NOPE", "en");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors")
                .asArray()
                .containsExactly(Map.of("field", "category", "code", "type-mismatch"));
        verify(listExercises, never()).list(any());
    }

    @Test
    @WithMockUser
    void shouldRefuseWithFieldError_whenNameIsLongerThan100() {
        var result = get("?name=" + "a".repeat(101), "en");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors")
                .asArray()
                .containsExactly(Map.of("field", "name", "code", "size"));
        verify(listExercises, never()).list(any());
    }

    @Test
    @WithMockUser
    void shouldHideInternals_whenListingFailsUnexpectedly() {
        when(listExercises.list(any())).thenThrow(new IllegalStateException("select * from exercises failed"));

        var result = get("", "en");

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("internal.error");
        assertThat(result)
                .bodyText()
                .doesNotContain("select", "IllegalStateException", "failed");
    }

    @Test
    void shouldRefuse_whenCallerIsAnonymous() {
        var result = get("", "en");

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
        verify(listExercises, never()).list(any());
    }
}
