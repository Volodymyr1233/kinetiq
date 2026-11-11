package com.kinetiq.planchange.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.exception.PlanChangeNothingToUndoException;
import com.kinetiq.planchange.domain.exception.PlanChangeWeekBegunException;
import com.kinetiq.planchange.domain.input.DecidePlanChangeUseCase;
import com.kinetiq.planchange.domain.input.PlanChangeSummary;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

@WebMvcTest(PlanChangeController.class)
@Import({SecurityConfig.class, WebConfig.class, PlanChangeWebMapperImpl.class})
class PlanChangeControllerTest {

    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final PlanChangeId ID = new PlanChangeId(UUID.fromString("00000000-0000-4000-9600-000000000001"));
    private static final ExerciseId SQUAT = new ExerciseId(UUID.fromString("00000000-0000-4000-9100-000000000011"));
    private static final String DETAIL = "/api/v1/plan-changes/" + ID.value();
    private static final String OPEN = "/api/v1/clients/" + CLIENT_ID.value() + "/plan-change";
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_ID.value().toString()).roles("CLIENT");

    private static final PlanChange PLAN_CHANGE = new PlanChange(
            ID,
            CLIENT_ID,
            Rule.HIGH_RPE,
            PlanChangeState.PENDING,
            3,
            LocalDate.parse("2026-10-26"),
            LocalDate.parse("2026-11-08"),
            LocalDate.parse("2026-11-16"),
            PlanChangeContent.ofChanges(List.of(new TargetChange(
                    new EntryRef(DayOfWeek.TUESDAY, 0),
                    SQUAT,
                    "Full body A",
                    Target.AMOUNT,
                    new BigDecimal("8.0"),
                    new BigDecimal("7.0")))),
            List.of(Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-10-26"), new BigDecimal("8.400"))),
            Set.of(SQUAT),
            Instant.parse("2026-11-09T02:00:00Z"),
            null,
            null,
            null,
            null);
    private static final PlanChangeText TEXT =
            new PlanChangeText("Back squat: cut volume by 13%", "Average RPE was 8.4 in the last 1 weeks, above 8.");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewPlanChangeUseCase planChanges;

    @MockitoBean
    private DecidePlanChangeUseCase decisions;

    private static void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
        assertThat(result).bodyJson().doesNotHavePath("$.detail");
    }

    @Test
    void shouldAnswerThePlanChange_withEveryChangedValueTheTextAndTheNumbers() {
        when(planChanges.get(TRAINER_ID, ID))
                .thenReturn(new PlanChangeView(PLAN_CHANGE, "Ola Nowak", TEXT, Map.of(SQUAT, new ExerciseLabel("Back squat", Measure.REPS))));

        MvcTestResult result = mvc.get().uri(DETAIL).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        var json = assertThat(result).bodyJson();
        json.extractingPath("$.id").isEqualTo(ID.value().toString());
        json.extractingPath("$.clientName").isEqualTo("Ola Nowak");
        json.extractingPath("$.rule").isEqualTo("HIGH_RPE");
        json.extractingPath("$.state").isEqualTo("PENDING");
        json.extractingPath("$.title").isEqualTo(TEXT.title());
        json.extractingPath("$.reason").isEqualTo(TEXT.reason());
        json.extractingPath("$.effectiveWeek").isEqualTo("2026-11-16");
        json.extractingPath("$.windowStart").isEqualTo("2026-10-26");
        json.extractingPath("$.targets[0].sessionDay").isEqualTo("TUESDAY");
        json.extractingPath("$.targets[0].sessionName").isEqualTo("Full body A");
        json.extractingPath("$.targets[0].position").isEqualTo(0);
        json.extractingPath("$.targets[0].exerciseName").isEqualTo("Back squat");
        json.extractingPath("$.targets[0].measure").isEqualTo("REPS");
        json.extractingPath("$.targets[0].target").isEqualTo("AMOUNT");
        json.extractingPath("$.targets[0].oldValue").isEqualTo(8.0);
        json.extractingPath("$.targets[0].newValue").isEqualTo(7.0);
        json.doesNotHavePath("$.removedSession");
        json.extractingPath("$.moves").asArray().isEmpty();
        json.extractingPath("$.concernedExercises[0].name").isEqualTo("Back squat");
        json.extractingPath("$.figures[0].kind").isEqualTo("MEAN_RPE");
        json.doesNotHavePath("$.figures[0].exerciseId");
        json.extractingPath("$.figures[0].value").isEqualTo(8.4);
    }

    @Test
    void shouldAnswerNotFound_forAnUnknownOrAnotherTrainersPlanChange() {
        when(planChanges.get(TRAINER_ID, ID)).thenThrow(new PlanChangeNotFoundException());

        assertRefused(mvc.get().uri(DETAIL).with(TRAINER).exchange(), HttpStatus.NOT_FOUND, "plan-change.not-found");
    }

    @Test
    void shouldAnswerTheOpenPlanChangeOfAClient() {
        when(planChanges.openOf(TRAINER_ID, CLIENT_ID)).thenReturn(new PlanChangeSummary(PLAN_CHANGE, TEXT));

        MvcTestResult result = mvc.get().uri(OPEN).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(ID.value().toString());
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo(TEXT.title());
        assertThat(result).bodyJson().extractingPath("$.effectiveWeek").isEqualTo("2026-11-16");
    }

    @Test
    void shouldAnswerNotFound_forAnotherTrainersClient_orNoOpenPlanChange() {
        when(planChanges.openOf(TRAINER_ID, CLIENT_ID)).thenThrow(new ClientNotFoundException());
        assertRefused(mvc.get().uri(OPEN).with(TRAINER).exchange(), HttpStatus.NOT_FOUND, "client.not-found");

        doThrow(new PlanChangeNotFoundException()).when(planChanges).openOf(TRAINER_ID, CLIENT_ID);
        assertRefused(mvc.get().uri(OPEN).with(TRAINER).exchange(), HttpStatus.NOT_FOUND, "plan-change.not-found");
    }

    private static final String ACCEPTANCE = DETAIL + "/acceptance";
    private static final PlanChange ACCEPTED =
            PLAN_CHANGE.accept(Side.TRAINER, Instant.parse("2026-11-12T08:00:00Z"), LocalDate.parse("2026-11-12"));

    private static PlanChangeView view(PlanChange planChange) {
        return new PlanChangeView(
                planChange, "Ola Nowak", TEXT, Map.of(SQUAT, new ExerciseLabel("Back squat", Measure.REPS)));
    }

    private MvcTestResult accept(RequestPostProcessor who) {
        return mvc.post().uri(ACCEPTANCE).with(who).with(csrf()).exchange();
    }

    private MvcTestResult undo(RequestPostProcessor who) {
        return mvc.delete().uri(ACCEPTANCE).with(who).with(csrf()).exchange();
    }

    @Test
    void shouldAnswerTheAcceptedPlanChange_withWhoAccepted() {
        when(decisions.accept(TRAINER_ID, ID)).thenReturn(view(ACCEPTED));

        MvcTestResult result = accept(TRAINER);

        assertThat(result).hasStatus(HttpStatus.OK);
        var json = assertThat(result).bodyJson();
        json.extractingPath("$.state").isEqualTo("ACCEPTED_BY_ONE");
        json.extractingPath("$.acceptedByTrainer").isEqualTo(true);
        json.extractingPath("$.acceptedByClient").isEqualTo(false);
        json.doesNotHavePath("$.declinedBy");
        json.doesNotHavePath("$.endedAt");
    }

    @Test
    void shouldAnswerTheUndonePlanChange() {
        when(decisions.undo(TRAINER_ID, ID)).thenReturn(view(PLAN_CHANGE));

        MvcTestResult result = undo(TRAINER);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.state").isEqualTo("PENDING");
        assertThat(result).bodyJson().extractingPath("$.acceptedByTrainer").isEqualTo(false);
    }

    @Test
    void shouldAnswerNotFound_whenDecidingOnAnUnknownOrAnotherTrainersPlanChange() {
        when(decisions.accept(TRAINER_ID, ID)).thenThrow(new PlanChangeNotFoundException());
        when(decisions.undo(TRAINER_ID, ID)).thenThrow(new PlanChangeNotFoundException());

        assertRefused(accept(TRAINER), HttpStatus.NOT_FOUND, "plan-change.not-found");
        assertRefused(undo(TRAINER), HttpStatus.NOT_FOUND, "plan-change.not-found");
    }

    @Test
    void shouldAnswerConflictWithItsCode_whenTheDecisionIsRefused() {
        when(decisions.accept(TRAINER_ID, ID)).thenThrow(new PlanChangeEndedException());
        when(decisions.undo(TRAINER_ID, ID)).thenThrow(new PlanChangeNothingToUndoException());

        assertRefused(accept(TRAINER), HttpStatus.CONFLICT, "plan-change.ended");
        assertRefused(undo(TRAINER), HttpStatus.CONFLICT, "plan-change.nothing-to-undo");

        doThrow(new PlanChangeWeekBegunException()).when(decisions).accept(TRAINER_ID, ID);
        assertRefused(accept(TRAINER), HttpStatus.CONFLICT, "plan-change.week-begun");
    }

    @Test
    void shouldRefuseTheDecisionsWithoutACsrfToken_aClient_andAnonymous_withoutCallingTheUseCase() {
        assertRefused(mvc.post().uri(ACCEPTANCE).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.forbidden");
        assertRefused(mvc.delete().uri(ACCEPTANCE).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.forbidden");
        assertRefused(accept(CLIENT), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(undo(CLIENT), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.post().uri(ACCEPTANCE).with(csrf()).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(decisions);
    }

    @Test
    void shouldRefuseAClient_onBothPaths() {
        assertRefused(mvc.get().uri(DETAIL).with(CLIENT).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.get().uri(OPEN).with(CLIENT).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        verifyNoInteractions(planChanges);
    }

    @Test
    void shouldAskToSignIn_whenNobodyIsSignedIn() {
        assertRefused(mvc.get().uri(DETAIL).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        assertRefused(mvc.get().uri(OPEN).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(planChanges);
    }
}
