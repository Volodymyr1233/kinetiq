package com.kinetiq.sessionlog.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.sessionlog.domain.exception.NoSessionTodayException;
import com.kinetiq.sessionlog.domain.exception.SessionLogNotFoundException;
import com.kinetiq.sessionlog.domain.exception.SetLogConflictException;
import com.kinetiq.sessionlog.domain.exception.SetValuesInvalidException;
import com.kinetiq.sessionlog.domain.input.LogSessionUseCase;
import com.kinetiq.sessionlog.domain.input.LogSetCommand;
import com.kinetiq.sessionlog.domain.input.StartedSession;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SetLog;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.model.SetValues;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(SessionLogController.class)
@Import({SecurityConfig.class, WebConfig.class, SessionLogWebMapper.class})
class SessionLogControllerTest {

    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-4000-a000-000000000001");
    private static final UUID SET_ID = UUID.fromString("00000000-0000-4000-a000-000000000002");
    private static final String SESSIONS = "/api/v1/session-logs";
    private static final String SET = SESSIONS + "/" + SESSION_ID + "/set-logs/" + SET_ID;
    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
    private static final RequestPostProcessor CLIENT =
            user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final RequestPostProcessor TRAINER =
            user(TRAINER_ID.value().toString()).roles("TRAINER");

    private static final Exercise SQUAT = new Exercise(
            new ExerciseId(UUID.fromString("00000000-0000-4000-9100-000000000011")),
            null,
            new LocalizedText("Back squat", "Przysiad ze sztangą"),
            Category.LEGS,
            new LocalizedText("", ""),
            Measure.REPS,
            3,
            5,
            120,
            new LocalizedText("", ""));

    private static final String BODY =
            """
            {"entryPosition":0,"setNumber":1,"loadKg":102.5,"amount":5,"rpe":8}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private LogSessionUseCase sessions;

    private static StartedSession started(boolean created, boolean withLoggedSet) {
        SessionLog log = new SessionLog(
                new com.kinetiq.sessionlog.domain.model.SessionLogId(SESSION_ID),
                CLIENT_ID,
                LocalDate.parse("2026-11-07"),
                "Lower body",
                NOW,
                null,
                List.of(new LoggedEntry(SQUAT.getId(), 2, 5, new BigDecimal("100.0"), 8, 120)),
                withLoggedSet
                        ? List.of(new SetLog(
                                new SetLogId(SET_ID),
                                0,
                                1,
                                new SetValues(null, 6, 9),
                                new BigDecimal("100.0"),
                                5,
                                8,
                                NOW))
                        : List.of(),
                0);
        return new StartedSession(log, Map.of(SQUAT.getId(), SQUAT), created);
    }

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    private MvcTestResult put(String body) {
        return mvc.put()
                .uri(SET)
                .with(CLIENT)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    @Test
    void shouldAnswerCreatedWithEveryPlannedSet_whenTheSessionIsStarted() {
        when(sessions.start(CLIENT_ID)).thenReturn(started(true, false));

        var result = mvc.post().uri(SESSIONS).with(CLIENT).with(csrf()).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(SESSION_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.date").isEqualTo("2026-11-07");
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Lower body");
        String entry = "$.entries[0]";
        assertThat(result).bodyJson().extractingPath(entry + ".position").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath(entry + ".exerciseName").isEqualTo("Back squat");
        assertThat(result).bodyJson().extractingPath(entry + ".measure").isEqualTo("REPS");
        assertThat(result).bodyJson().extractingPath(entry + ".restSeconds").isEqualTo(120);
        assertThat(result).bodyJson().extractingPath(entry + ".sets[0].number").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath(entry + ".sets[1].number").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath(entry + ".sets[0].target.loadKg").isEqualTo(100.0);
        assertThat(result).bodyJson().extractingPath(entry + ".sets[0].target.amount").isEqualTo(5);
        assertThat(result).bodyJson().extractingPath(entry + ".sets[0].target.rpe").isEqualTo(8);
        assertThat(result).bodyJson().doesNotHavePath(entry + ".sets[0].log");
    }

    @Test
    void shouldAnswerOkWithTheLoggedSets_whenTheSessionWasStartedAlready() {
        when(sessions.start(CLIENT_ID)).thenReturn(started(false, true));

        var result = mvc.post().uri(SESSIONS).with(CLIENT).with(csrf()).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        String log = "$.entries[0].sets[0].log";
        assertThat(result).bodyJson().extractingPath(log + ".id").isEqualTo(SET_ID.toString());
        assertThat(result).bodyJson().extractingPath(log + ".amount").isEqualTo(6);
        assertThat(result).bodyJson().extractingPath(log + ".rpe").isEqualTo(9);
        assertThat(result).bodyJson().doesNotHavePath(log + ".loadKg");
        assertThat(result).bodyJson().doesNotHavePath("$.entries[0].sets[1].log");
    }

    @Test
    void shouldNameExercisesInTheLanguageOfTheRequest() {
        when(sessions.start(CLIENT_ID)).thenReturn(started(true, false));

        var result = mvc.post()
                .uri(SESSIONS)
                .header("Accept-Language", "pl")
                .with(CLIENT)
                .with(csrf())
                .exchange();

        assertThat(result).bodyJson().extractingPath("$.entries[0].exerciseName").isEqualTo("Przysiad ze sztangą");
    }

    @Test
    void shouldRefuseToStart_whenThereIsNoPlanOrNoSessionToday() {
        doThrow(new PlanNotFoundException()).when(sessions).start(CLIENT_ID);
        assertRefused(
                mvc.post().uri(SESSIONS).with(CLIENT).with(csrf()).exchange(),
                HttpStatus.NOT_FOUND,
                "plan.not-found");

        doThrow(new NoSessionTodayException()).when(sessions).start(CLIENT_ID);
        assertRefused(
                mvc.post().uri(SESSIONS).with(CLIENT).with(csrf()).exchange(),
                HttpStatus.CONFLICT,
                "session-log.no-session-today");
    }

    @Test
    void shouldLogASetForTheSignedInClient_andAnswerItsPlace() {
        SetLog logged = new SetLog(
                new SetLogId(SET_ID), 0, 1, new SetValues(new BigDecimal("102.5"), 5, 8), null, 5, 8, NOW);
        when(sessions.logSet(any())).thenReturn(logged);

        var result = put(BODY);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(SET_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.entryPosition").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.setNumber").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.loadKg").isEqualTo(102.5);
        ArgumentCaptor<LogSetCommand> command = ArgumentCaptor.forClass(LogSetCommand.class);
        verify(sessions).logSet(command.capture());
        assertThat(command.getValue().client()).isEqualTo(CLIENT_ID);
        assertThat(command.getValue().session().value()).isEqualTo(SESSION_ID);
        assertThat(command.getValue().setLogId().value()).isEqualTo(SET_ID);
        assertThat(command.getValue().loadKg()).isEqualByComparingTo("102.5");
    }

    @Test
    void shouldMapTheRefusalsOfALoggedSet() {
        doThrow(new SessionLogNotFoundException()).when(sessions).logSet(any());
        assertRefused(put(BODY), HttpStatus.NOT_FOUND, "session-log.not-found");

        doThrow(new SetValuesInvalidException("amount")).when(sessions).logSet(any());
        var invalid = put(BODY);
        assertRefused(invalid, HttpStatus.BAD_REQUEST, "set-values.invalid");
        assertThat(invalid).bodyJson().extractingPath("$.field").isEqualTo("amount");

        doThrow(SetLogConflictException.idInUse()).when(sessions).logSet(any());
        assertRefused(put(BODY), HttpStatus.CONFLICT, "set-log.id-in-use");

        doThrow(SetLogConflictException.placeTaken()).when(sessions).logSet(any());
        assertRefused(put(BODY), HttpStatus.CONFLICT, "set-log.place-taken");
    }

    @Test
    void shouldRefuseABodyThatCannotBeRead() {
        assertRefused(
                put("{\"entryPosition\":0,\"setNumber\":1,\"amount\":\"many\",\"rpe\":8}"),
                HttpStatus.BAD_REQUEST,
                "request.invalid");
        assertRefused(put("{\"entryPosition\":0,\"setNumber\":1,\"rpe\":8}"), HttpStatus.BAD_REQUEST, "validation.failed");
    }

    @Test
    void shouldRefuseATrainer_onBothOperations() {
        assertRefused(
                mvc.post().uri(SESSIONS).with(TRAINER).with(csrf()).exchange(),
                HttpStatus.FORBIDDEN,
                "request.not-allowed");
        assertRefused(
                mvc.put()
                        .uri(SET)
                        .with(TRAINER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .exchange(),
                HttpStatus.FORBIDDEN,
                "request.not-allowed");
        verifyNoInteractions(sessions);
    }

    @Test
    void shouldAskToSignIn_whenNobodyIsSignedIn() {
        assertRefused(
                mvc.post().uri(SESSIONS).with(csrf()).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        assertRefused(
                mvc.put()
                        .uri(SET)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .exchange(),
                HttpStatus.UNAUTHORIZED,
                "request.unauthenticated");
        verifyNoInteractions(sessions);
    }
}
