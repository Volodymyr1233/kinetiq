package com.kinetiq.overview.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.overview.domain.input.Overview;
import com.kinetiq.overview.domain.input.Overview.Alert;
import com.kinetiq.overview.domain.input.Overview.Kind;
import com.kinetiq.overview.domain.input.Overview.OpenPlanChange;
import com.kinetiq.overview.domain.input.Overview.Row;
import com.kinetiq.overview.domain.input.Overview.Tiles;
import com.kinetiq.overview.domain.input.Overview.Waiting;
import com.kinetiq.overview.domain.input.ViewOverviewUseCase;
import com.kinetiq.overview.domain.model.ClientStatus;
import com.kinetiq.overview.domain.model.Completion;
import com.kinetiq.overview.domain.model.PlanDates;
import com.kinetiq.overview.domain.model.StatusReason;
import com.kinetiq.overview.domain.model.Verdict;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.readiness.domain.model.Readiness;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

@WebMvcTest(OverviewController.class)
@Import({SecurityConfig.class, WebConfig.class, OverviewWebMapper.class})
class OverviewControllerTest {

    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final AccountId DRAFT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000006"));
    private static final PlanChangeId PLAN_CHANGE = PlanChangeId.random();
    private static final String OVERVIEW = "/api/v1/clients/overview";
    private static final RequestPostProcessor TRAINER =
            user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT =
            user(CLIENT_ID.value().toString()).roles("CLIENT");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewOverviewUseCase overview;

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    // a client At risk in week 6 of 8 with everything present, and one with a draft and nothing else
    private static Overview overview() {
        ZoneId warsaw = ZoneId.of("Europe/Warsaw");
        return new Overview(
                new Tiles(1, 2, 2, 3, new BigDecimal("7.8"), new BigDecimal("-0.2"), 1, 1),
                List.of(
                        new Alert(
                                Kind.AT_RISK,
                                CLIENT_ID,
                                new OpenPlanChange(PLAN_CHANGE, "Cut volume by 10%", Waiting.TRAINER),
                                null,
                                null,
                                null,
                                null),
                        new Alert(Kind.NO_LOG, CLIENT_ID, null, null, 7, LocalDate.parse("2026-11-05"), "Lower body"),
                        Alert.of(Kind.RECOVERY_DAY, CLIENT_ID)),
                List.of(
                        new Row(
                                new ClientSummary(CLIENT_ID, "Anna Kowalska", Goal.BUILD_MUSCLE, PlanState.PUBLISHED, warsaw),
                                new PlanDates(
                                        LocalDate.parse("2026-10-05"),
                                        LocalDate.parse("2026-10-07"),
                                        8,
                                        Set.of(DayOfWeek.MONDAY)),
                                6,
                                new Verdict(ClientStatus.AT_RISK, Set.of(StatusReason.HIGH_RPE, StatusReason.LOW_COMPLETION)),
                                new Completion(5, 12),
                                new BigDecimal("8.4"),
                                Instant.parse("2026-11-05T17:00:00Z"),
                                new Readiness(25, Readiness.Band.RECOVERY_DAY),
                                true),
                        new Row(
                                new ClientSummary(DRAFT_ID, "Bartek Nowak", null, PlanState.DRAFT, warsaw),
                                null,
                                null,
                                new Verdict(ClientStatus.PLAN_TO_REVIEW, Set.of()),
                                null,
                                null,
                                null,
                                null,
                                false)));
    }

    @Test
    void shouldAnswerTheTilesAlertsAndRows_ofTheSignedInTrainer() {
        when(overview.overview(TRAINER_ID)).thenReturn(overview());

        var result = mvc.get().uri(OVERVIEW).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        var json = assertThat(result).bodyJson();
        json.extractingPath("$.activeClients").isEqualTo(1);
        json.extractingPath("$.averageRpeChange").isEqualTo(-0.2);
        json.extractingPath("$.planChangesWaiting").isEqualTo(1);
        json.extractingPath("$.alerts[0].kind").isEqualTo("AT_RISK");
        json.extractingPath("$.alerts[0].planChangeId").isEqualTo(PLAN_CHANGE.value().toString());
        json.extractingPath("$.alerts[0].waitingFor").isEqualTo("TRAINER");
        json.extractingPath("$.alerts[1].daysWithoutLog").isEqualTo(7);
        json.extractingPath("$.alerts[1].since").isEqualTo("2026-11-05");
        json.extractingPath("$.alerts[1].lastSessionName").isEqualTo("Lower body");
        json.extractingPath("$.clients[0].planWeek").isEqualTo(6);
        json.extractingPath("$.clients[0].planWeeks").isEqualTo(8);
        json.extractingPath("$.clients[0].planEndsOn").isEqualTo("2026-11-29");
        json.extractingPath("$.clients[0].reasons").isEqualTo(List.of("LOW_COMPLETION", "HIGH_RPE"));
        json.extractingPath("$.clients[0].sessionsDue").isEqualTo(12);
        json.extractingPath("$.clients[0].lastLoggedAt").isEqualTo("2026-11-05T17:00:00Z");
        json.extractingPath("$.clients[0].readinessBand").isEqualTo("RECOVERY_DAY");
        json.extractingPath("$.clients[0].lighterTargets").isEqualTo(true);
    }

    @Test
    void shouldLeaveOutWhatIsAbsent() {
        when(overview.overview(TRAINER_ID)).thenReturn(overview());

        var result = mvc.get().uri(OVERVIEW).with(TRAINER).exchange();

        var json = assertThat(result).bodyJson();
        json.extractingPath("$.alerts[2]").isEqualTo(Map.of("kind", "RECOVERY_DAY", "clientId", CLIENT_ID.value().toString()));
        json.extractingPath("$.clients[1]")
                .isEqualTo(Map.of(
                        "id", DRAFT_ID.value().toString(),
                        "name", "Bartek Nowak",
                        "planState", "DRAFT",
                        "status", "PLAN_TO_REVIEW",
                        "reasons", List.of(),
                        "lighterTargets", false));
    }

    @Test
    void shouldRefuseAClientAndAVisitor() {
        assertRefused(mvc.get().uri(OVERVIEW).with(CLIENT).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(mvc.get().uri(OVERVIEW).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        verifyNoInteractions(overview);
    }
}
