package com.kinetiq.planchange.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.exception.PlanChangeNothingToUndoException;
import com.kinetiq.planchange.domain.exception.PlanChangeWeekBegunException;
import com.kinetiq.planchange.domain.input.ClientPlanChangeView;
import com.kinetiq.planchange.domain.input.DecideOwnPlanChangeUseCase;
import com.kinetiq.planchange.domain.input.KeepCurrentPlanCommand;
import com.kinetiq.planchange.domain.input.ViewOwnPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.DeclineReason;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ClientPlanChangeController.class)
@Import({SecurityConfig.class, WebConfig.class, PlanChangeWebMapperImpl.class})
class ClientPlanChangeControllerTest {

    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final PlanChangeId ID = new PlanChangeId(UUID.fromString("00000000-0000-4000-9600-000000000001"));
    private static final ExerciseId SQUAT = new ExerciseId(UUID.fromString("00000000-0000-4000-9100-000000000011"));
    private static final String ACCEPTANCE = "/api/v1/plan/changes/" + ID.value() + "/acceptance";
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
    private static final PlanChange ACCEPTED =
            PLAN_CHANGE.accept(Side.CLIENT, Instant.parse("2026-11-12T08:00:00Z"), LocalDate.parse("2026-11-12"));
    private static final PlanChangeText TEXT =
            new PlanChangeText("Back squat: cut volume by 13%", "Average RPE was 8.4 in the last 1 weeks, above 8.");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private DecideOwnPlanChangeUseCase decisions;

    @MockitoBean
    private ViewOwnPlanChangeUseCase planChanges;

    private static ClientPlanChangeView view(PlanChange planChange) {
        return new ClientPlanChangeView(
                planChange, "Marek", TEXT, Map.of(SQUAT, new ExerciseLabel("Back squat", Measure.REPS)));
    }

    private static void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
        assertThat(result).bodyJson().doesNotHavePath("$.detail");
    }

    private MvcTestResult accept(RequestPostProcessor who) {
        return mvc.post().uri(ACCEPTANCE).with(who).with(csrf()).exchange();
    }

    private MvcTestResult undo(RequestPostProcessor who) {
        return mvc.delete().uri(ACCEPTANCE).with(who).with(csrf()).exchange();
    }

    @Test
    void shouldAnswerTheAcceptedPlanChange_withTheTrainersNameAndWhoAccepted() {
        when(decisions.accept(CLIENT_ID, ID)).thenReturn(view(ACCEPTED));

        MvcTestResult result = accept(CLIENT);

        assertThat(result).hasStatus(HttpStatus.OK);
        var json = assertThat(result).bodyJson();
        json.extractingPath("$.id").isEqualTo(ID.value().toString());
        json.extractingPath("$.counterpartName").isEqualTo("Marek");
        json.doesNotHavePath("$.clientName");
        json.extractingPath("$.state").isEqualTo("ACCEPTED_BY_ONE");
        json.extractingPath("$.acceptedByClient").isEqualTo(true);
        json.extractingPath("$.acceptedByTrainer").isEqualTo(false);
        json.extractingPath("$.title").isEqualTo(TEXT.title());
        json.extractingPath("$.targets[0].exerciseName").isEqualTo("Back squat");
        json.extractingPath("$.targets[0].oldValue").isEqualTo(8.0);
        json.extractingPath("$.figures[0].kind").isEqualTo("MEAN_RPE");
    }

    @Test
    void shouldAnswerTheUndonePlanChange() {
        when(decisions.undo(CLIENT_ID, ID)).thenReturn(view(PLAN_CHANGE));

        MvcTestResult result = undo(CLIENT);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.state").isEqualTo("PENDING");
        assertThat(result).bodyJson().extractingPath("$.acceptedByClient").isEqualTo(false);
    }

    @Test
    void shouldAnswerNotFound_forAnUnknownOrAnotherClientsPlanChange() {
        when(decisions.accept(CLIENT_ID, ID)).thenThrow(new PlanChangeNotFoundException());
        when(decisions.undo(CLIENT_ID, ID)).thenThrow(new PlanChangeNotFoundException());

        assertRefused(accept(CLIENT), HttpStatus.NOT_FOUND, "plan-change.not-found");
        assertRefused(undo(CLIENT), HttpStatus.NOT_FOUND, "plan-change.not-found");
    }

    @Test
    void shouldAnswerConflictWithItsCode_whenTheDecisionIsRefused() {
        when(decisions.accept(CLIENT_ID, ID)).thenThrow(new PlanChangeEndedException());
        when(decisions.undo(CLIENT_ID, ID)).thenThrow(new PlanChangeNothingToUndoException());

        assertRefused(accept(CLIENT), HttpStatus.CONFLICT, "plan-change.ended");
        assertRefused(undo(CLIENT), HttpStatus.CONFLICT, "plan-change.nothing-to-undo");

        doThrow(new PlanChangeWeekBegunException()).when(decisions).accept(CLIENT_ID, ID);
        assertRefused(accept(CLIENT), HttpStatus.CONFLICT, "plan-change.week-begun");
    }

    @Test
    void shouldRefuseWithoutACsrfToken_aTrainer_andAnonymous_withoutCallingTheUseCase() {
        assertRefused(mvc.post().uri(ACCEPTANCE).with(CLIENT).exchange(), HttpStatus.FORBIDDEN, "request.forbidden");
        assertRefused(mvc.delete().uri(ACCEPTANCE).with(CLIENT).exchange(), HttpStatus.FORBIDDEN, "request.forbidden");
        assertRefused(accept(TRAINER), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(undo(TRAINER), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.post().uri(ACCEPTANCE).with(csrf()).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(decisions);
    }

    private static final String CURRENT = "/api/v1/plan/changes/current";
    private static final String DETAIL = "/api/v1/plan/changes/" + ID.value();

    @Test
    void shouldAnswerTheCurrentPlanChange_andOneByIdWithTheSameBody() {
        when(planChanges.current(CLIENT_ID)).thenReturn(view(PLAN_CHANGE));
        when(planChanges.get(CLIENT_ID, ID)).thenReturn(view(PLAN_CHANGE));

        for (String path : new String[] {CURRENT, DETAIL}) {
            MvcTestResult result = mvc.get().uri(path).with(CLIENT).exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            var json = assertThat(result).bodyJson();
            json.extractingPath("$.id").isEqualTo(ID.value().toString());
            json.extractingPath("$.counterpartName").isEqualTo("Marek");
            json.extractingPath("$.title").isEqualTo(TEXT.title());
            json.extractingPath("$.effectiveWeek").isEqualTo("2026-11-16");
            json.extractingPath("$.targets[0].newValue").isEqualTo(7.0);
        }
    }

    @Test
    void shouldAnswerNotFound_whenThereIsNoCurrentPlanChange_orTheOneAskedForIsNotTheClients() {
        when(planChanges.current(CLIENT_ID)).thenThrow(new PlanChangeNotFoundException());
        when(planChanges.get(CLIENT_ID, ID)).thenThrow(new PlanChangeNotFoundException());

        assertRefused(mvc.get().uri(CURRENT).with(CLIENT).exchange(), HttpStatus.NOT_FOUND, "plan-change.not-found");
        assertRefused(mvc.get().uri(DETAIL).with(CLIENT).exchange(), HttpStatus.NOT_FOUND, "plan-change.not-found");
    }

    @Test
    void shouldRefuseTheReads_forATrainer_andAnonymous() {
        assertRefused(mvc.get().uri(CURRENT).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.get().uri(DETAIL).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.get().uri(CURRENT).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(planChanges);
    }

    private static final String DECLINATION = "/api/v1/plan/changes/" + ID.value() + "/declination";

    private MvcTestResult keep(RequestPostProcessor who, String body) {
        return mvc.post()
                .uri(DECLINATION)
                .with(who)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    @Test
    void shouldKeepTheCurrentPlan_andAnswerWhoDeclinedAndWhy() {
        PlanChange declined = PLAN_CHANGE.decline(
                Side.CLIENT,
                DeclineReason.INJURY,
                "Knee",
                Instant.parse("2026-11-12T08:00:00Z"),
                LocalDate.parse("2026-11-12"));
        when(decisions.keepCurrent(new KeepCurrentPlanCommand(CLIENT_ID, ID, DeclineReason.INJURY, "Knee")))
                .thenReturn(view(declined));

        MvcTestResult result = keep(CLIENT, "{\"reason\":\"INJURY\",\"note\":\"Knee\"}");

        assertThat(result).hasStatus(HttpStatus.OK);
        var json = assertThat(result).bodyJson();
        json.extractingPath("$.state").isEqualTo("DECLINED");
        json.extractingPath("$.declinedBy").isEqualTo("CLIENT");
        json.extractingPath("$.declineReason").isEqualTo("INJURY");
        json.extractingPath("$.declineNote").isEqualTo("Knee");
        json.extractingPath("$.counterpartName").isEqualTo("Marek");
    }

    @Test
    void shouldRefuseAMissingReason_aNoteOf501Characters_andAnUnknownReason() {
        for (String body : new String[] {
            "{}", "{\"reason\":\"OTHER\",\"note\":\"" + "x".repeat(501) + "\"}", "{\"reason\":\"BORED\"}"
        }) {
            assertThat(keep(CLIENT, body)).hasStatus(HttpStatus.BAD_REQUEST);
        }
        verifyNoInteractions(decisions);
    }

    @Test
    void shouldAnswerConflictOrNotFound_whenKeepingTheCurrentPlanIsRefused() {
        doThrow(new PlanChangeEndedException()).when(decisions).keepCurrent(any());
        assertRefused(keep(CLIENT, "{\"reason\":\"OTHER\"}"), HttpStatus.CONFLICT, "plan-change.ended");

        doThrow(new PlanChangeNotFoundException()).when(decisions).keepCurrent(any());
        assertRefused(keep(CLIENT, "{\"reason\":\"OTHER\"}"), HttpStatus.NOT_FOUND, "plan-change.not-found");
    }

    @Test
    void shouldRefuseKeepingWithoutACsrfToken_aTrainer_andAnonymous() {
        assertRefused(
                mvc.post().uri(DECLINATION).with(CLIENT).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"OTHER\"}").exchange(),
                HttpStatus.FORBIDDEN,
                "request.forbidden");
        assertRefused(keep(TRAINER, "{\"reason\":\"OTHER\"}"), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(
                mvc.post().uri(DECLINATION).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"OTHER\"}").exchange(),
                HttpStatus.UNAUTHORIZED,
                "request.unauthenticated");
        verifyNoInteractions(decisions);
    }
}
