package com.kinetiq.client.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ReviewClientAnswersUseCase;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.quiz.domain.exception.QuizNotFoundException;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ClientQuizAnswersController.class)
@Import({SecurityConfig.class, WebConfig.class, ClientWebMapperImpl.class})
class ClientQuizAnswersControllerTest {

    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final String ADDRESS = "/api/v1/clients/" + CLIENT_ID.value() + "/quiz-answers";
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_ID.value().toString()).roles("CLIENT");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ReviewClientAnswersUseCase answers;

    private static QuizSubmission submission(String notes) {
        return new QuizSubmission(
                QuizSubmissionId.random(),
                CLIENT_ID,
                Goal.BUILD_MUSCLE,
                Experience.YEARS_1_TO_2,
                Level.INTERMEDIATE,
                29,
                168,
                new BigDecimal("62.5"),
                Set.of(Like.YOGA_PILATES, Like.WEIGHTS),
                3,
                notes,
                Instant.parse("2026-11-05T10:00:00Z"));
    }

    @Test
    void shouldAnswerTheClientsAnswers() {
        when(answers.answers(TRAINER_ID, CLIENT_ID)).thenReturn(submission("Left knee gets stiff"));

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        var body = assertThat(result).bodyJson();
        body.extractingPath("$.goal").isEqualTo("BUILD_MUSCLE");
        body.extractingPath("$.experience").isEqualTo("YEARS_1_TO_2");
        body.extractingPath("$.level").isEqualTo("INTERMEDIATE");
        body.extractingPath("$.age").isEqualTo(29);
        body.extractingPath("$.heightCm").isEqualTo(168);
        body.extractingPath("$.weightKg").isEqualTo(62.5);
        body.extractingPath("$.likes").isEqualTo(List.of("WEIGHTS", "YOGA_PILATES"));
        body.extractingPath("$.daysPerWeek").isEqualTo(3);
        body.extractingPath("$.notes").isEqualTo("Left knee gets stiff");
        body.extractingPath("$.submittedAt").isEqualTo("2026-11-05T10:00:00Z");
    }

    @Test
    void shouldLeaveOutTheNotes_whenThereAreNone() {
        when(answers.answers(TRAINER_ID, CLIENT_ID)).thenReturn(submission(null));

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().doesNotHavePath("$.notes");
    }

    @Test
    void shouldAnswerClientNotFound_whenTheClientIsUnknownOrAnotherTrainers() {
        when(answers.answers(TRAINER_ID, CLIENT_ID)).thenThrow(new ClientNotFoundException());

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("client.not-found");
    }

    @Test
    void shouldAnswerQuizNotFound_whenTheClientHasNoAnswers() {
        when(answers.answers(TRAINER_ID, CLIENT_ID)).thenThrow(new QuizNotFoundException());

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("quiz.not-found");
    }

    @Test
    void shouldRefuse_whenCallerIsClientOrNobodyIsSignedIn() {
        assertThat(mvc.get().uri(ADDRESS).with(CLIENT).exchange()).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri(ADDRESS).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(answers);
    }
}
