package com.kinetiq.sessionlog.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanWeekOutOfRangeException;
import com.kinetiq.sessionlog.domain.input.SessionStates;
import com.kinetiq.sessionlog.domain.input.ViewSessionStatesUseCase;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SessionState;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

@WebMvcTest(SessionStateController.class)
@Import({SecurityConfig.class, WebConfig.class, SessionLogWebMapper.class})
class SessionStateControllerTest {

    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final UUID LOG_ID = UUID.fromString("00000000-0000-4000-a000-000000000001");
    private static final String STATES = "/api/v1/session-states";
    private static final RequestPostProcessor CLIENT =
            user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final RequestPostProcessor TRAINER =
            user(TRAINER_ID.value().toString()).roles("TRAINER");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewSessionStatesUseCase states;

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    @Test
    void shouldAnswerTheStateOfEachSession_leavingOutWhatIsMissing() {
        when(states.states(CLIENT_ID, 3))
                .thenReturn(new SessionStates(
                        LocalDate.parse("2026-11-04"),
                        List.of(
                                new SessionStates.Entry(
                                        DayOfWeek.MONDAY,
                                        LocalDate.parse("2026-11-02"),
                                        Optional.of(SessionState.DONE),
                                        Optional.of(new SessionLogId(LOG_ID)),
                                        Optional.of(Instant.parse("2026-11-02T08:00:00Z"))),
                                new SessionStates.Entry(
                                        DayOfWeek.TUESDAY,
                                        LocalDate.parse("2026-11-03"),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty()),
                                new SessionStates.Entry(
                                        DayOfWeek.WEDNESDAY,
                                        LocalDate.parse("2026-11-04"),
                                        Optional.of(SessionState.TODAY),
                                        Optional.empty(),
                                        Optional.empty()))));

        var result = mvc.get().uri(STATES).param("week", "3").with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.today").isEqualTo("2026-11-04");
        assertThat(result).bodyJson().extractingPath("$.sessions[0].day").isEqualTo("MONDAY");
        assertThat(result).bodyJson().extractingPath("$.sessions[0].date").isEqualTo("2026-11-02");
        assertThat(result).bodyJson().extractingPath("$.sessions[0].state").isEqualTo("DONE");
        assertThat(result).bodyJson().extractingPath("$.sessions[0].sessionLogId").isEqualTo(LOG_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.sessions[0].startedAt").isEqualTo("2026-11-02T08:00:00Z");
        assertThat(result).bodyJson().doesNotHavePath("$.sessions[1].state");
        assertThat(result).bodyJson().doesNotHavePath("$.sessions[1].sessionLogId");
        assertThat(result).bodyJson().doesNotHavePath("$.sessions[1].startedAt");
        assertThat(result).bodyJson().extractingPath("$.sessions[2].state").isEqualTo("TODAY");
        assertThat(result).bodyJson().doesNotHavePath("$.sessions[2].sessionLogId");
        assertThat(result).bodyJson().doesNotHavePath("$.sessions[2].startedAt");
    }

    @Test
    void shouldAskForTheCurrentWeek_whenNoWeekIsGiven() {
        when(states.states(CLIENT_ID, null)).thenReturn(new SessionStates(LocalDate.parse("2026-11-04"), List.of()));

        var result = mvc.get().uri(STATES).with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.sessions").asList().isEmpty();
    }

    @Test
    void shouldMapTheRefusals() {
        doThrow(new PlanNotFoundException()).when(states).states(CLIENT_ID, null);
        assertRefused(mvc.get().uri(STATES).with(CLIENT).exchange(), HttpStatus.NOT_FOUND, "plan.not-found");

        doThrow(new PlanWeekOutOfRangeException()).when(states).states(CLIENT_ID, 99);
        assertRefused(
                mvc.get().uri(STATES).param("week", "99").with(CLIENT).exchange(),
                HttpStatus.BAD_REQUEST,
                "plan.week-out-of-range");
    }

    @Test
    void shouldRefuseATrainerAndAVisitor() {
        assertRefused(mvc.get().uri(STATES).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.get().uri(STATES).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(states);
    }
}
