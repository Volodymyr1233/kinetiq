package com.kinetiq.plan.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Session;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

@WebMvcTest(TodayController.class)
@Import({SecurityConfig.class, WebConfig.class, PlanWebMapperImpl.class})
class TodayControllerTest {

    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final String ADDRESS = "/api/v1/today";
    private static final LocalDate DATE = LocalDate.parse("2026-11-05");
    private static final RequestPostProcessor CLIENT =
            user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final RequestPostProcessor TRAINER =
            user(TRAINER_ID.value().toString()).roles("TRAINER");

    private static final Exercise PLANK = new Exercise(
            new ExerciseId(UUID.fromString("00000000-0000-4000-9100-000000000026")),
            null,
            new LocalizedText("Plank", "Deska"),
            Category.CORE,
            new LocalizedText("", ""),
            Measure.SECONDS,
            3,
            45,
            60,
            new LocalizedText("", ""));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewOwnPlanUseCase plans;

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    @Test
    void shouldAnswerTheSessionWithItsTargets_inTheRequestsLanguage() {
        Session session = new Session(
                "Core", DayOfWeek.THURSDAY, List.of(new ExerciseEntry(PLANK.getId(), 3, 45, null, 7, 60)));
        when(plans.today(CLIENT_ID))
                .thenReturn(new TodayView(DATE, TodayView.State.SESSION, session, Map.of(PLANK.getId(), PLANK)));

        var result =
                mvc.get().uri(ADDRESS).header("Accept-Language", "pl").with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.date").isEqualTo("2026-11-05");
        assertThat(result).bodyJson().extractingPath("$.state").isEqualTo("SESSION");
        assertThat(result).bodyJson().extractingPath("$.session.name").isEqualTo("Core");
        String entry = "$.session.entries[0]";
        assertThat(result).bodyJson().extractingPath(entry + ".exerciseName").isEqualTo("Deska");
        assertThat(result).bodyJson().extractingPath(entry + ".measure").isEqualTo("SECONDS");
        assertThat(result).bodyJson().extractingPath(entry + ".sets").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath(entry + ".amount").isEqualTo(45);
        assertThat(result).bodyJson().doesNotHavePath(entry + ".loadKg");
    }

    @Test
    void shouldLeaveOutTheSession_onARestDayAndOnceThePlanHasFinished() {
        when(plans.today(CLIENT_ID)).thenReturn(new TodayView(DATE, TodayView.State.REST, null, Map.of()));
        var rest = mvc.get().uri(ADDRESS).with(CLIENT).exchange();
        assertThat(rest).hasStatus(HttpStatus.OK);
        assertThat(rest).bodyJson().extractingPath("$.state").isEqualTo("REST");
        assertThat(rest).bodyJson().doesNotHavePath("$.session");

        when(plans.today(CLIENT_ID)).thenReturn(new TodayView(DATE, TodayView.State.FINISHED, null, Map.of()));
        var finished = mvc.get().uri(ADDRESS).with(CLIENT).exchange();
        assertThat(finished).bodyJson().extractingPath("$.state").isEqualTo("FINISHED");
        assertThat(finished).bodyJson().doesNotHavePath("$.session");
    }

    @Test
    void shouldAnswerNotFound_whenThereIsNoPublishedPlan() {
        when(plans.today(CLIENT_ID)).thenThrow(new PlanNotFoundException());

        assertRefused(mvc.get().uri(ADDRESS).with(CLIENT).exchange(), HttpStatus.NOT_FOUND, "plan.not-found");
    }

    @Test
    void shouldRefuseATrainer() {
        assertRefused(mvc.get().uri(ADDRESS).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        verifyNoInteractions(plans);
    }

    @Test
    void shouldAskToSignIn_whenNobodyIsSignedIn() {
        assertRefused(mvc.get().uri(ADDRESS).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(plans);
    }
}
