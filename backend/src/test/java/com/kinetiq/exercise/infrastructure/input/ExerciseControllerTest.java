package com.kinetiq.exercise.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.exercise.domain.exception.ExerciseLimitReachedException;
import com.kinetiq.exercise.domain.exception.ExerciseNameTakenException;
import com.kinetiq.exercise.domain.exception.ExerciseNotFoundException;
import com.kinetiq.exercise.domain.input.CreateExerciseCommand;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.input.ManageExercisesUseCase;
import com.kinetiq.exercise.domain.input.UpdateExerciseCommand;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ExerciseController.class)
@Import({SecurityConfig.class, WebConfig.class, ExerciseWebMapperImpl.class})
class ExerciseControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-9100-000000000026");
    private static final UUID OWN_ID = UUID.fromString("00000000-0000-4000-9200-000000000001");
    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT =
            user("00000000-0000-4000-8000-000000000003").roles("CLIENT");

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

    private static final Exercise GOBLET_SQUAT = new Exercise(
            new ExerciseId(OWN_ID),
            TRAINER_ID,
            new LocalizedText("Goblet squat", ""),
            Category.LEGS,
            new LocalizedText("Dumbbell", ""),
            Measure.REPS,
            3,
            10,
            90,
            new LocalizedText("Elbows inside the knees.", ""));

    private static final String BODY =
            """
            {"name":"Goblet squat","category":"LEGS","measure":"REPS","equipment":"Dumbbell",
             "sets":3,"amount":10,"restSeconds":90,"cues":"Elbows inside the knees."}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListExercisesUseCase listExercises;

    @MockitoBean
    private ManageExercisesUseCase manageExercises;

    @BeforeEach
    void stubUseCases() {
        when(listExercises.list(any())).thenReturn(new ExerciseListing(List.of(new ExerciseListing.Entry(PLANK, 0)), 40));
        when(manageExercises.create(any())).thenReturn(new ExerciseListing.Entry(GOBLET_SQUAT, 0));
        when(manageExercises.update(any())).thenReturn(new ExerciseListing.Entry(GOBLET_SQUAT, 0));
    }

    private MvcTestResult get(String query, String language) {
        return mvc.get()
                .uri("/api/v1/exercises" + query)
                .with(TRAINER)
                .header("Accept-Language", language)
                .exchange();
    }

    private MvcTestResult post(String json, RequestPostProcessor user) {
        return mvc.post()
                .uri("/api/v1/exercises")
                .with(csrf())
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult put(String json, RequestPostProcessor user) {
        return mvc.put()
                .uri("/api/v1/exercises/{id}", OWN_ID)
                .with(csrf())
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static String bodyWith(String field, String json) {
        return BODY.replaceFirst("\"" + field + "\":(\"[^\"]*\"|\\d+)", "\"" + field + "\":" + json);
    }

    private ListExercisesQuery passedQuery() {
        ArgumentCaptor<ListExercisesQuery> query = ArgumentCaptor.forClass(ListExercisesQuery.class);
        verify(listExercises).list(query.capture());
        return query.getValue();
    }

    private static void assertFieldError(MvcTestResult result, String field, String code) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors")
                .asArray()
                .containsExactly(Map.of("field", field, "code", code));
    }

    @Nested
    class ListExercises {

        @Test
        void shouldPassTrainerAndFiltersToUseCase_whenTheyAreGiven() {
            assertThat(get("?category=CORE&name=pla", "en")).hasStatusOk();

            assertThat(passedQuery())
                    .isEqualTo(new ListExercisesQuery(TRAINER_ID, Category.CORE, "pla", Locale.ENGLISH));
        }

        @Test
        void shouldPassNoFilters_whenParametersAreAbsent() {
            assertThat(get("", "en")).hasStatusOk();

            assertThat(passedQuery()).isEqualTo(new ListExercisesQuery(TRAINER_ID, null, "", Locale.ENGLISH));
        }

        @Test
        void shouldAnswerInPolishWithEnglishFallback_whenPolishIsAsked() {
            var result = get("", "pl-PL,pl;q=0.9");

            assertThat(passedQuery().locale()).isEqualTo(Locale.forLanguageTag("pl"));
            assertThat(result).bodyJson().extractingPath("$.exercises[0].name").isEqualTo("Deska");
            assertThat(result).bodyJson().extractingPath("$.exercises[0].cues").isEqualTo("Żebra w dół.");
            assertThat(result).bodyJson().extractingPath("$.exercises[0].equipment").isEqualTo("Bodyweight");
        }

        @Test
        void shouldAnswerInEnglish_whenLanguageIsUnsupported() {
            var result = get("", "de-DE");

            assertThat(passedQuery().locale()).isEqualTo(Locale.ENGLISH);
            assertThat(result).bodyJson().extractingPath("$.exercises[0].name").isEqualTo("Plank");
        }

        @Test
        void shouldReturnExerciseTotalAndPlans_whenListed() {
            var result = get("", "en");

            assertThat(result).bodyJson().extractingPath("$.total").isEqualTo(40);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.exercises[0]")
                    .isEqualTo(Map.ofEntries(
                            Map.entry("id", ID.toString()),
                            Map.entry("name", "Plank"),
                            Map.entry("category", "CORE"),
                            Map.entry("equipment", "Bodyweight"),
                            Map.entry("measure", "SECONDS"),
                            Map.entry("sets", 3),
                            Map.entry("amount", 45),
                            Map.entry("restSeconds", 60),
                            Map.entry("cues", "Ribs down."),
                            Map.entry("plansUsing", 0),
                            Map.entry("starter", true)));
        }

        @Test
        void shouldRefuseWithFieldError_whenCategoryIsUnknown() {
            var result = get("?category=NOPE", "en");

            assertFieldError(result, "category", "type-mismatch");
            verify(listExercises, never()).list(any());
        }

        @Test
        void shouldRefuseWithFieldError_whenNameIsLongerThan100() {
            var result = get("?name=" + "a".repeat(101), "en");

            assertFieldError(result, "name", "size");
            verify(listExercises, never()).list(any());
        }

        @Test
        void shouldHideInternals_whenListingFailsUnexpectedly() {
            when(listExercises.list(any())).thenThrow(new IllegalStateException("select * from exercises failed"));

            var result = get("", "en");

            assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("internal.error");
            assertThat(result).bodyText().doesNotContain("select", "IllegalStateException", "failed");
        }

        @Test
        void shouldRefuse_whenCallerIsAnonymous() {
            var result = mvc.get().uri("/api/v1/exercises").exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
            verify(listExercises, never()).list(any());
        }

        @Test
        void shouldRefuse_whenCallerIsClient() {
            var result = mvc.get().uri("/api/v1/exercises").with(CLIENT).exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
            assertThat(result).bodyJson().doesNotHavePath("$.exercises");
            verify(listExercises, never()).list(any());
        }
    }

    @Nested
    class Create {

        @Test
        void shouldAnswerCreatedOwnExercise_whenBodyIsValid() {
            var result = post(BODY, TRAINER);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$")
                    .isEqualTo(Map.ofEntries(
                            Map.entry("id", OWN_ID.toString()),
                            Map.entry("name", "Goblet squat"),
                            Map.entry("category", "LEGS"),
                            Map.entry("equipment", "Dumbbell"),
                            Map.entry("measure", "REPS"),
                            Map.entry("sets", 3),
                            Map.entry("amount", 10),
                            Map.entry("restSeconds", 90),
                            Map.entry("cues", "Elbows inside the knees."),
                            Map.entry("plansUsing", 0),
                            Map.entry("starter", false)));
            verify(manageExercises)
                    .create(new CreateExerciseCommand(
                            TRAINER_ID,
                            "Goblet squat",
                            Category.LEGS,
                            Measure.REPS,
                            "Dumbbell",
                            3,
                            10,
                            90,
                            "Elbows inside the knees."));
        }

        @Test
        void shouldIgnoreOwnerIdAndStarter_whenBodyCarriesThem() {
            String body = BODY.replace(
                    "{", "{\"owner\":\"00000000-0000-4000-8000-000000000004\",\"id\":\"%s\",\"starter\":true,"
                            .formatted(ID));

            assertThat(post(body, TRAINER)).hasStatus(HttpStatus.CREATED);

            ArgumentCaptor<CreateExerciseCommand> command = ArgumentCaptor.forClass(CreateExerciseCommand.class);
            verify(manageExercises).create(command.capture());
            assertThat(command.getValue().trainer()).isEqualTo(TRAINER_ID);
        }

        @ParameterizedTest
        @CsvSource(
                delimiter = '|',
                value = {
                    "name        | '\"  \"'  | not-blank",
                    "name        | '\"%s\"'  | size",
                    "category    | null      | not-null",
                    "measure     | null      | not-null",
                    "equipment   | '\"%e\"'  | size",
                    "cues        | '\"%c\"'  | size",
                    "sets        | 0         | min",
                    "sets        | 21        | max",
                    "amount      | 0         | min",
                    "amount      | 3601      | max",
                    "restSeconds | -1        | min",
                    "restSeconds | 601       | max"
                })
        void shouldAnswerFieldError_whenValueBreaksARule(String field, String json, String code) {
            String value = json.replace("%s", "a".repeat(101))
                    .replace("%e", "a".repeat(51))
                    .replace("%c", "a".repeat(501));

            assertFieldError(post(bodyWith(field, value), TRAINER), field, code);
            verify(manageExercises, never()).create(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"\"category\":\"NECK\"", "\"sets\":\"three\"", "\"measure\":\"MINUTES\""})
        void shouldAnswerInvalidWithoutServerText_whenValueCannotBeRead(String replacement) {
            String body = BODY.replaceFirst("\"" + replacement.split("\"")[1] + "\":(\"[^\"]*\"|\\d+)", replacement);

            var result = post(body, TRAINER);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.invalid");
            assertThat(result).bodyJson().doesNotHavePath("$.detail");
            assertThat(result).bodyText().doesNotContain("Category", "Measure", "NECK", "three", "Exception");
            verify(manageExercises, never()).create(any());
        }

        @Test
        void shouldAnswerConflict_whenNameIsTaken() {
            when(manageExercises.create(any())).thenThrow(new ExerciseNameTakenException());

            var result = post(BODY, TRAINER);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("exercise.name-taken");
        }

        @Test
        void shouldAnswerConflict_whenLimitIsReached() {
            when(manageExercises.create(any())).thenThrow(new ExerciseLimitReachedException());

            var result = post(BODY, TRAINER);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("exercise.limit-reached");
        }

        @Test
        void shouldHideInternals_whenCreatingFailsUnexpectedly() {
            when(manageExercises.create(any())).thenThrow(new RuntimeException("insert into exercises failed"));

            var result = post(BODY, TRAINER);

            assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("internal.error");
            assertThat(result).bodyText().doesNotContain("insert", "RuntimeException", "failed");
        }

        @Test
        void shouldRefuse_whenCallerIsClient() {
            var result = post(BODY, CLIENT);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
            verifyNoInteractions(manageExercises);
        }

        @Test
        void shouldRefuse_whenCallerIsAnonymous() {
            var result = mvc.post()
                    .uri("/api/v1/exercises")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
            verifyNoInteractions(manageExercises);
        }

        @Test
        void shouldRefuse_whenCsrfTokenIsMissing() {
            var result = mvc.post()
                    .uri("/api/v1/exercises")
                    .with(TRAINER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.forbidden");
            verifyNoInteractions(manageExercises);
        }
    }

    @Nested
    class Update {

        private static final String UPDATE_BODY = BODY.replace("\"measure\":\"REPS\",", "");

        @Test
        void shouldAnswerUpdatedExercise_whenBodyIsValid() {
            var result = put(UPDATE_BODY, TRAINER);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.starter").isEqualTo(false);
            verify(manageExercises)
                    .update(new UpdateExerciseCommand(
                            TRAINER_ID,
                            new ExerciseId(OWN_ID),
                            "Goblet squat",
                            Category.LEGS,
                            "Dumbbell",
                            3,
                            10,
                            90,
                            "Elbows inside the knees."));
        }

        @Test
        void shouldIgnoreMeasure_whenBodyCarriesIt() {
            assertThat(put(BODY.replace("REPS", "SECONDS"), TRAINER)).hasStatusOk();
        }

        @Test
        void shouldAnswerFieldError_whenSetsAreOutOfRange() {
            assertFieldError(put(UPDATE_BODY.replace("\"sets\":3", "\"sets\":21"), TRAINER), "sets", "max");
            verify(manageExercises, never()).update(any());
        }

        @Test
        void shouldAnswerNotFound_whenExerciseIsNotTheTrainersOwn() {
            when(manageExercises.update(any())).thenThrow(new ExerciseNotFoundException());

            var result = put(UPDATE_BODY, TRAINER);

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("exercise.not-found");
        }

        @Test
        void shouldAnswerConflict_whenNameIsTaken() {
            when(manageExercises.update(any())).thenThrow(new ExerciseNameTakenException());

            var result = put(UPDATE_BODY, TRAINER);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("exercise.name-taken");
        }

        @Test
        void shouldReadTheSameInPolish_whenOwnExerciseHasNoPolishText() {
            var result = mvc.put()
                    .uri("/api/v1/exercises/{id}", OWN_ID)
                    .with(csrf())
                    .with(TRAINER)
                    .header("Accept-Language", "pl")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(UPDATE_BODY)
                    .exchange();

            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Goblet squat");
            assertThat(result).bodyJson().extractingPath("$.cues").isEqualTo("Elbows inside the knees.");
        }

        @Test
        void shouldRefuse_whenCallerIsClient() {
            var result = put(UPDATE_BODY, CLIENT);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
            verifyNoInteractions(manageExercises);
        }

        @Test
        void shouldRefuse_whenCallerIsAnonymous() {
            var result = mvc.put()
                    .uri("/api/v1/exercises/{id}", OWN_ID)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(UPDATE_BODY)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
            verifyNoInteractions(manageExercises);
        }

        @Test
        void shouldRefuse_whenCsrfTokenIsMissing() {
            var result = mvc.put()
                    .uri("/api/v1/exercises/{id}", OWN_ID)
                    .with(TRAINER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(UPDATE_BODY)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.forbidden");
            verifyNoInteractions(manageExercises);
        }
    }

    @Nested
    class Delete {

        @ParameterizedTest
        @ValueSource(strings = {"/api/v1/exercises/00000000-0000-4000-9200-000000000001", "/api/v1/exercises"})
        void shouldAnswerMethodNotAllowed_whenDeleteIsAsked(String uri) {
            var result = mvc.delete().uri(uri).with(csrf()).with(TRAINER).exchange();

            assertThat(result).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.method-not-allowed");
            verifyNoInteractions(manageExercises);
        }
    }
}
