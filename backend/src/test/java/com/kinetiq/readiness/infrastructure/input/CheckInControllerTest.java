package com.kinetiq.readiness.infrastructure.input;

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
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.readiness.domain.exception.NoSessionTodayException;
import com.kinetiq.readiness.domain.exception.TrainingStartedException;
import com.kinetiq.readiness.domain.input.CheckInUseCase;
import com.kinetiq.readiness.domain.input.SaveCheckInCommand;
import com.kinetiq.readiness.domain.input.TodayCheckIn;
import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.CheckInId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

@WebMvcTest(CheckInController.class)
@Import({SecurityConfig.class, WebConfig.class, CheckInWebMapper.class})
class CheckInControllerTest {

    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final String TODAY = "/api/v1/check-ins/today";
    private static final LocalDate DATE = LocalDate.parse("2026-11-08");
    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");
    private static final RequestPostProcessor CLIENT =
            user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final RequestPostProcessor TRAINER =
            user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final String BODY = """
            {"sleep":4,"soreness":2,"stress":1}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CheckInUseCase checkIns;

    private static TodayCheckIn checkedIn() {
        return new TodayCheckIn(
                DATE, true, new CheckIn(CheckInId.random(), CLIENT_ID, DATE, new Answers(4, 2, 1), NOW));
    }

    private MvcTestResult put(String body) {
        return mvc.put()
                .uri(TODAY)
                .with(CLIENT)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    @Test
    void shouldAnswerTheDayWithoutACheckIn_whenTheClientHasNotCheckedIn() {
        when(checkIns.today(CLIENT_ID)).thenReturn(new TodayCheckIn(DATE, true, null));

        var result = mvc.get().uri(TODAY).with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.date").isEqualTo("2026-11-08");
        assertThat(result).bodyJson().extractingPath("$.open").isEqualTo(true);
        assertThat(result).bodyJson().doesNotHavePath("$.checkIn");
    }

    @Test
    void shouldAnswerTheAnswersAndTheirReadiness_whenTheClientCheckedIn() {
        when(checkIns.today(CLIENT_ID)).thenReturn(checkedIn());

        var result = mvc.get().uri(TODAY).with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.checkIn.sleep").isEqualTo(4);
        assertThat(result).bodyJson().extractingPath("$.checkIn.soreness").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.checkIn.stress").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.checkIn.score").isEqualTo(83);
        assertThat(result).bodyJson().extractingPath("$.checkIn.band").isEqualTo("READY");
        assertThat(result).bodyJson().extractingPath("$.checkIn.savedAt").isEqualTo("2026-11-08T08:00:00Z");
    }

    @Test
    void shouldSaveTheAnswersForTheSignedInClient() {
        when(checkIns.save(any())).thenReturn(checkedIn());

        var result = put(BODY);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.checkIn.score").isEqualTo(83);
        ArgumentCaptor<SaveCheckInCommand> command = ArgumentCaptor.forClass(SaveCheckInCommand.class);
        verify(checkIns).save(command.capture());
        assertThat(command.getValue()).isEqualTo(new SaveCheckInCommand(CLIENT_ID, new Answers(4, 2, 1)));
    }

    @ParameterizedTest(name = "{0} gives {1} {2}")
    @CsvSource({
        "'{\"soreness\":2,\"stress\":1}',            sleep,    not-null",
        "'{\"sleep\":0,\"soreness\":2,\"stress\":1}', sleep,    min",
        "'{\"sleep\":4,\"soreness\":6,\"stress\":1}', soreness, max",
        "'{\"sleep\":4,\"soreness\":2,\"stress\":9}', stress,   max"
    })
    void shouldRefuseAnAnswerMissingOrOutsideOneToFive_withItsField(String body, String field, String code) {
        var result = put(body);

        assertRefused(result, HttpStatus.BAD_REQUEST, "validation.failed");
        assertThat(result).bodyJson().extractingPath("$.errors[0]").isEqualTo(Map.of("field", field, "code", code));
        verifyNoInteractions(checkIns);
    }

    @Test
    void shouldRefuseABodyThatCannotBeRead() {
        assertRefused(put("{\"sleep\":2.5,\"soreness\":2,\"stress\":1}"), HttpStatus.BAD_REQUEST, "request.invalid");
        assertRefused(put("{\"sleep\":\"well\",\"soreness\":2,\"stress\":1}"), HttpStatus.BAD_REQUEST, "request.invalid");
        verifyNoInteractions(checkIns);
    }

    @Test
    void shouldMapTheRefusals() {
        doThrow(new PlanNotFoundException()).when(checkIns).today(CLIENT_ID);
        assertRefused(mvc.get().uri(TODAY).with(CLIENT).exchange(), HttpStatus.NOT_FOUND, "plan.not-found");

        doThrow(new PlanNotFoundException()).when(checkIns).save(any());
        assertRefused(put(BODY), HttpStatus.NOT_FOUND, "plan.not-found");

        doThrow(new NoSessionTodayException()).when(checkIns).save(any());
        assertRefused(put(BODY), HttpStatus.CONFLICT, "check-in.no-session-today");

        doThrow(new TrainingStartedException()).when(checkIns).save(any());
        assertRefused(put(BODY), HttpStatus.CONFLICT, "check-in.training-started");
    }

    @Test
    void shouldRefuseASaveWithoutTheCsrfToken() {
        var result = mvc.put()
                .uri(TODAY)
                .with(CLIENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(checkIns);
    }

    @Test
    void shouldRefuseATrainer_onBothOperations() {
        assertRefused(mvc.get().uri(TODAY).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(
                mvc.put()
                        .uri(TODAY)
                        .with(TRAINER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .exchange(),
                HttpStatus.FORBIDDEN,
                "request.not-allowed");
        verifyNoInteractions(checkIns);
    }

    @Test
    void shouldAskToSignIn_whenNobodyIsSignedIn() {
        assertRefused(mvc.get().uri(TODAY).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        assertRefused(
                mvc.put()
                        .uri(TODAY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .exchange(),
                HttpStatus.UNAUTHORIZED,
                "request.unauthenticated");
        verifyNoInteractions(checkIns);
    }
}
