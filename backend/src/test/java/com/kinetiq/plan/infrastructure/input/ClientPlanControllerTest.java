package com.kinetiq.plan.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.kinetiq.plan.domain.exception.PlanWeekOutOfRangeException;
import com.kinetiq.plan.domain.input.DraftSummary;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
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

@WebMvcTest(ClientPlanController.class)
@Import({SecurityConfig.class, WebConfig.class, PlanWebMapperImpl.class})
class ClientPlanControllerTest {

    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final String ADDRESS = "/api/v1/plan";
    private static final String ACKNOWLEDGEMENT = ADDRESS + "/acknowledgement";
    private static final String DRAFT_SUMMARY = ADDRESS + "/draft-summary";
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

    private static OwnPlanView view(OwnPlanView.Week week, boolean finished, BigDecimal load) {
        Session session = new Session(
                "Core", DayOfWeek.THURSDAY, List.of(new ExerciseEntry(PLANK.getId(), 3, 45, load, 7, 60)));
        Plan plan = new Plan(
                PlanId.random(),
                CLIENT_ID,
                Goal.LOSE_FAT,
                12,
                LocalDate.parse("2026-09-28"),
                List.of(session),
                4);
        return new OwnPlanView(plan, week, finished ? 14 : 6, finished, false, Map.of(PLANK.getId(), PLANK));
    }

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    @Test
    void shouldAnswerTheWeekAndItsSessions_whenPublished() {
        when(plans.view(CLIENT_ID, null))
                .thenReturn(view(new OwnPlanView.Week(6, LocalDate.parse("2026-11-02")), false, null));

        var result = mvc.get().uri(ADDRESS).with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.goal").isEqualTo("LOSE_FAT");
        assertThat(result).bodyJson().extractingPath("$.weeks").isEqualTo(12);
        assertThat(result).bodyJson().extractingPath("$.currentWeek").isEqualTo(6);
        assertThat(result).bodyJson().extractingPath("$.finished").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.seen").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.week.number").isEqualTo(6);
        assertThat(result).bodyJson().extractingPath("$.week.startsOn").isEqualTo("2026-11-02");
        assertThat(result).bodyJson().extractingPath("$.sessions[0].name").isEqualTo("Core");
        assertThat(result).bodyJson().extractingPath("$.sessions[0].day").isEqualTo("THURSDAY");
        String entry = "$.sessions[0].entries[0]";
        assertThat(result).bodyJson().extractingPath(entry + ".exerciseName").isEqualTo("Plank");
        assertThat(result).bodyJson().extractingPath(entry + ".measure").isEqualTo("SECONDS");
        assertThat(result).bodyJson().extractingPath(entry + ".amount").isEqualTo(45);
        assertThat(result).bodyJson().doesNotHavePath(entry + ".loadKg");
    }

    @Test
    void shouldAskForTheWeekInTheQueryAndNameExercisesInTheRequestsLanguage() {
        when(plans.view(CLIENT_ID, 3))
                .thenReturn(view(new OwnPlanView.Week(3, LocalDate.parse("2026-10-12")), false, new BigDecimal("5.0")));

        var result = mvc.get()
                .uri(ADDRESS + "?week=3")
                .header("Accept-Language", "pl")
                .with(CLIENT)
                .exchange();

        assertThat(result).bodyJson().extractingPath("$.week.number").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.sessions[0].entries[0].exerciseName").isEqualTo("Deska");
        assertThat(result).bodyJson().extractingPath("$.sessions[0].entries[0].loadKg").isEqualTo(5.0);
    }

    @Test
    void shouldLeaveOutTheWeek_whenThePlanHasFinished() {
        when(plans.view(CLIENT_ID, null)).thenReturn(view(null, true, null));

        var result = mvc.get().uri(ADDRESS).with(CLIENT).exchange();

        assertThat(result).bodyJson().extractingPath("$.finished").isEqualTo(true);
        assertThat(result).bodyJson().doesNotHavePath("$.week");
    }

    @Test
    void shouldAnswerNotFound_whenThereIsNoPublishedPlan() {
        when(plans.view(CLIENT_ID, null)).thenThrow(new PlanNotFoundException());

        assertRefused(mvc.get().uri(ADDRESS).with(CLIENT).exchange(), HttpStatus.NOT_FOUND, "plan.not-found");
    }

    @Test
    void shouldRefuseAWeekThePlanDoesNotHave() {
        when(plans.view(CLIENT_ID, 99)).thenThrow(new PlanWeekOutOfRangeException());

        assertRefused(
                mvc.get().uri(ADDRESS + "?week=99").with(CLIENT).exchange(),
                HttpStatus.BAD_REQUEST,
                "plan.week-out-of-range");
    }

    @Test
    void shouldRefuseAWeekThatIsNotANumber() {
        assertRefused(
                mvc.get().uri(ADDRESS + "?week=next").with(CLIENT).exchange(),
                HttpStatus.BAD_REQUEST,
                "request.invalid");
        verifyNoInteractions(plans);
    }

    @Test
    void shouldMarkThePlanSeenForTheSignedInClient() {
        var result = mvc.post().uri(ACKNOWLEDGEMENT).with(CLIENT).with(csrf()).exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(plans).acknowledge(CLIENT_ID);
    }

    @Test
    void shouldAnswerNotFound_whenThereIsNoPublishedPlanToMark() {
        doThrow(new PlanNotFoundException()).when(plans).acknowledge(CLIENT_ID);

        assertRefused(
                mvc.post().uri(ACKNOWLEDGEMENT).with(CLIENT).with(csrf()).exchange(),
                HttpStatus.NOT_FOUND,
                "plan.not-found");
    }

    @Test
    void shouldRefuseATrainer_onBothOperations() {
        assertRefused(mvc.get().uri(ADDRESS).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(
                mvc.post().uri(ACKNOWLEDGEMENT).with(TRAINER).with(csrf()).exchange(),
                HttpStatus.FORBIDDEN,
                "request.not-allowed");
        verifyNoInteractions(plans);
    }

    @Test
    void shouldAskToSignIn_whenNobodyIsSignedIn() {
        assertRefused(mvc.get().uri(ADDRESS).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        assertRefused(
                mvc.post().uri(ACKNOWLEDGEMENT).with(csrf()).exchange(),
                HttpStatus.UNAUTHORIZED,
                "request.unauthenticated");
        verifyNoInteractions(plans);
    }

    @Test
    void shouldAnswerTheSummaryOfTheSignedInClientsDraft_andNothingElseOfIt() {
        when(plans.draftSummary(CLIENT_ID))
                .thenReturn(new DraftSummary(List.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), 12, Goal.LOSE_FAT));

        var result = mvc.get().uri(DRAFT_SUMMARY).with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().isStrictlyEqualTo("""
                {"trainingDays": ["MONDAY", "THURSDAY"], "weeks": 12, "goal": "LOSE_FAT"}
                """);
    }

    @Test
    void shouldAnswerNotFound_whenThereIsNoDraftSummary() {
        when(plans.draftSummary(CLIENT_ID)).thenThrow(new PlanNotFoundException());

        assertRefused(mvc.get().uri(DRAFT_SUMMARY).with(CLIENT).exchange(), HttpStatus.NOT_FOUND, "plan.not-found");
    }

    @Test
    void shouldRefuseATrainerAndAskAVisitorToSignIn_onTheDraftSummary() {
        assertRefused(
                mvc.get().uri(DRAFT_SUMMARY).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.get().uri(DRAFT_SUMMARY).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(plans);
    }
}
