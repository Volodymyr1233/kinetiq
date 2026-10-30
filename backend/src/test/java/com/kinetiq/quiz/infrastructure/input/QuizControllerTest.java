package com.kinetiq.quiz.infrastructure.input;

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
import com.kinetiq.quiz.domain.exception.QuizAlreadySubmittedException;
import com.kinetiq.quiz.domain.input.SubmitQuizCommand;
import com.kinetiq.quiz.domain.input.SubmitQuizUseCase;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
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

@WebMvcTest(QuizController.class)
@Import({SecurityConfig.class, WebConfig.class, QuizWebMapperImpl.class})
class QuizControllerTest {

    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final RequestPostProcessor CLIENT = user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final RequestPostProcessor TRAINER =
            user("00000000-0000-4000-8000-000000000001").roles("TRAINER");

    private static final String BODY =
            """
            {"goal":"BUILD_MUSCLE","experience":"YEARS_1_TO_2","level":"INTERMEDIATE","age":29,"heightCm":168,
             "weightKg":62.5,"likes":["WEIGHTS","YOGA_PILATES"],"daysPerWeek":3,"notes":"Left knee"}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SubmitQuizUseCase quiz;

    @BeforeEach
    void stubUseCase() {
        when(quiz.submit(any())).thenReturn(QuizStatus.WAITING_FOR_PLAN);
        when(quiz.status(any())).thenReturn(QuizStatus.NOT_SUBMITTED);
    }

    private MvcTestResult post(String json, RequestPostProcessor user) {
        return mvc.post()
                .uri("/api/v1/quiz")
                .with(csrf())
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static String bodyWith(String field, String json) {
        return BODY.replaceFirst("\"" + field + "\":(\"[^\"]*\"|[\\d.]+|\\[[^\\]]*\\])", "\"" + field + "\":" + json);
    }

    @Nested
    class Status {

        @Test
        void shouldAnswerTheSignedInClientsStatus() {
            var result = mvc.get().uri("/api/v1/quiz").with(CLIENT).exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$").isEqualTo(Map.of("status", "NOT_SUBMITTED"));
            verify(quiz).status(CLIENT_ID);
        }

        @Test
        void shouldRefuse_whenCallerIsAnonymous() {
            var result = mvc.get().uri("/api/v1/quiz").exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
            verifyNoInteractions(quiz);
        }

        @Test
        void shouldRefuse_whenCallerIsTrainer() {
            var result = mvc.get().uri("/api/v1/quiz").with(TRAINER).exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
            verifyNoInteractions(quiz);
        }
    }

    @Nested
    class Submit {

        @Test
        void shouldAnswerCreatedWithStatus_whenBodyIsValid() {
            var result = post(BODY, CLIENT);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson().extractingPath("$").isEqualTo(Map.of("status", "WAITING_FOR_PLAN"));
            verify(quiz)
                    .submit(new SubmitQuizCommand(
                            CLIENT_ID,
                            Goal.BUILD_MUSCLE,
                            Experience.YEARS_1_TO_2,
                            Level.INTERMEDIATE,
                            29,
                            168,
                            new BigDecimal("62.5"),
                            Set.of(Like.WEIGHTS, Like.YOGA_PILATES),
                            3,
                            "Left knee"));
        }

        @Test
        void shouldPassNoNotes_whenNotesAreAbsent() {
            assertThat(post(BODY.replace(",\"notes\":\"Left knee\"", ""), CLIENT)).hasStatus(HttpStatus.CREATED);

            ArgumentCaptor<SubmitQuizCommand> command = ArgumentCaptor.forClass(SubmitQuizCommand.class);
            verify(quiz).submit(command.capture());
            assertThat(command.getValue().notes()).isNull();
        }

        @ParameterizedTest
        @CsvSource(
                delimiter = '|',
                value = {
                    "goal        | null    | not-null",
                    "experience  | null    | not-null",
                    "level       | null    | not-null",
                    "age         | 15      | min",
                    "age         | 100     | max",
                    "heightCm    | 119     | min",
                    "heightCm    | 231     | max",
                    "weightKg    | null    | not-null",
                    "weightKg    | 29.9    | decimal-min",
                    "weightKg    | 250.1   | decimal-max",
                    "weightKg    | 62.55   | digits",
                    "likes       | []      | not-empty",
                    "daysPerWeek | 1       | min",
                    "daysPerWeek | 7       | max",
                    "notes       | '\"%n\"' | size"
                })
        void shouldAnswerFieldError_whenValueBreaksARule(String field, String json, String code) {
            var result = post(bodyWith(field, json.replace("%n", "a".repeat(501))), CLIENT);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.errors")
                    .asArray()
                    .containsExactly(Map.of("field", field, "code", code));
            verify(quiz, never()).submit(any());
        }

        @ParameterizedTest
        @ValueSource(
                strings = {"\"goal\":\"SWIM\"", "\"likes\":[\"SWIMMING\"]", "\"likes\":[\"WEIGHTS\",null]", "\"age\":\"twenty\""})
        void shouldAnswerInvalidWithoutServerText_whenValueCannotBeRead(String replacement) {
            String field = replacement.split("\"")[1];

            var result = post(bodyWith(field, replacement.substring(replacement.indexOf(':') + 1)), CLIENT);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.invalid");
            assertThat(result).bodyJson().doesNotHavePath("$.detail");
            assertThat(result).bodyText().doesNotContain("Goal", "Like", "SWIM", "twenty", "Exception");
            verify(quiz, never()).submit(any());
        }

        @Test
        void shouldAnswerConflict_whenClientSubmittedBefore() {
            when(quiz.submit(any())).thenThrow(new QuizAlreadySubmittedException());

            var result = post(BODY, CLIENT);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("quiz.already-submitted");
        }

        @Test
        void shouldHideInternals_whenSubmittingFailsUnexpectedly() {
            when(quiz.submit(any()))
                    .thenThrow(new IllegalStateException("insert into quiz_submissions failed"));

            var result = post(BODY, CLIENT);

            assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("internal.error");
            assertThat(result).bodyText().doesNotContain("insert", "quiz_submissions", "IllegalStateException", "failed");
        }

        @Test
        void shouldRefuse_whenCallerIsAnonymous() {
            var result = mvc.post()
                    .uri("/api/v1/quiz")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            verifyNoInteractions(quiz);
        }

        @Test
        void shouldRefuse_whenCallerIsTrainer() {
            var result = post(BODY, TRAINER);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
            verifyNoInteractions(quiz);
        }
    }
}
