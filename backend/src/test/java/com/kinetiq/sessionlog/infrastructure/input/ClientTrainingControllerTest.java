package com.kinetiq.sessionlog.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import com.kinetiq.sessionlog.domain.input.ClientOverview;
import com.kinetiq.sessionlog.domain.input.ClientSession;
import com.kinetiq.sessionlog.domain.input.ViewClientTrainingUseCase;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SessionState;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ClientTrainingController.class)
@Import({SecurityConfig.class, WebConfig.class, SessionLogWebMapper.class})
class ClientTrainingControllerTest {

    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final UUID LOG_ID = UUID.fromString("00000000-0000-4000-a000-000000000001");
    private static final String OVERVIEW = "/api/v1/clients/" + CLIENT_ID.value() + "/overview";
    private static final RequestPostProcessor TRAINER =
            user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT =
            user(CLIENT_ID.value().toString()).roles("CLIENT");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewClientTrainingUseCase training;

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    private static ClientSession done() {
        return new ClientSession(
                LocalDate.parse("2026-11-02"),
                "Upper body",
                SessionState.DONE,
                true,
                5,
                5,
                new BigDecimal("8.2"),
                new SessionLogId(LOG_ID));
    }

    private static ClientSession neverStarted(LocalDate date, SessionState state) {
        return new ClientSession(date, "Lower body", state, false, 0, 5, null, null);
    }

    private static QuizSubmission answers() {
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
                "Left knee gets stiff",
                Instant.parse("2026-10-15T10:00:00Z"));
    }

    @Test
    void shouldAnswerTheWeekTheRecentSessionsAndTheQuizAnswers() {
        when(training.overview(TRAINER_ID, CLIENT_ID))
                .thenReturn(new ClientOverview(
                        LocalDate.parse("2026-11-04"),
                        new ClientOverview.Week(
                                3,
                                LocalDate.parse("2026-11-02"),
                                List.of(
                                        done(),
                                        neverStarted(LocalDate.parse("2026-11-04"), SessionState.TODAY))),
                        List.of(done(), neverStarted(LocalDate.parse("2026-10-28"), SessionState.MISSED)),
                        answers()));

        var result = mvc.get().uri(OVERVIEW).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.today").isEqualTo("2026-11-04");
        assertThat(result).bodyJson().extractingPath("$.week.number").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.week.startsOn").isEqualTo("2026-11-02");
        assertThat(result).bodyJson().extractingPath("$.week.sessions[0].day").isEqualTo("MONDAY");
        assertThat(result).bodyJson().extractingPath("$.week.sessions[0].name").isEqualTo("Upper body");
        assertThat(result).bodyJson().extractingPath("$.week.sessions[0].state").isEqualTo("DONE");
        assertThat(result).bodyJson().extractingPath("$.week.sessions[0].locked").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.week.sessions[0].setsLogged").isEqualTo(5);
        assertThat(result).bodyJson().extractingPath("$.week.sessions[0].setsPlanned").isEqualTo(5);
        assertThat(result).bodyJson().extractingPath("$.week.sessions[0].averageRpe").isEqualTo(8.2);
        assertThat(result).bodyJson().extractingPath("$.week.sessions[0].sessionLogId").isEqualTo(LOG_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.week.sessions[1].state").isEqualTo("TODAY");
        assertThat(result).bodyJson().doesNotHavePath("$.week.sessions[1].averageRpe");
        assertThat(result).bodyJson().doesNotHavePath("$.week.sessions[1].sessionLogId");
        assertThat(result).bodyJson().extractingPath("$.recentSessions[1].date").isEqualTo("2026-10-28");
        assertThat(result).bodyJson().extractingPath("$.recentSessions[1].day").isEqualTo("WEDNESDAY");
        assertThat(result).bodyJson().extractingPath("$.recentSessions[1].state").isEqualTo("MISSED");
        assertThat(result).bodyJson().extractingPath("$.recentSessions[1].setsLogged").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.quizAnswers.goal").isEqualTo("BUILD_MUSCLE");
        assertThat(result).bodyJson().extractingPath("$.quizAnswers.weightKg").isEqualTo(62.5);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.quizAnswers.likes")
                .asList()
                .containsExactly("WEIGHTS", "YOGA_PILATES");
        assertThat(result).bodyJson().extractingPath("$.quizAnswers.notes").isEqualTo("Left knee gets stiff");
    }

    @Test
    void shouldLeaveOutTheWeekTheQuizAnswersAndAMissingState() {
        when(training.overview(TRAINER_ID, CLIENT_ID))
                .thenReturn(new ClientOverview(
                        LocalDate.parse("2026-11-04"),
                        null,
                        List.of(neverStarted(LocalDate.parse("2026-11-02"), null)),
                        null));

        var result = mvc.get().uri(OVERVIEW).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().doesNotHavePath("$.week");
        assertThat(result).bodyJson().doesNotHavePath("$.quizAnswers");
        assertThat(result).bodyJson().doesNotHavePath("$.recentSessions[0].state");
    }

    @Test
    void shouldAnswerNotFound_forAClientWhoIsNotTheTrainersOwn() {
        when(training.overview(TRAINER_ID, CLIENT_ID)).thenThrow(new ClientNotFoundException());

        assertRefused(mvc.get().uri(OVERVIEW).with(TRAINER).exchange(), HttpStatus.NOT_FOUND, "client.not-found");
    }

    @Test
    void shouldRefuseAClientAndAVisitor() {
        assertRefused(mvc.get().uri(OVERVIEW).with(CLIENT).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.get().uri(OVERVIEW).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(training);
    }
}
