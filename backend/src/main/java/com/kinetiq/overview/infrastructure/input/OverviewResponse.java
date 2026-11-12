package com.kinetiq.overview.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.overview.domain.input.Overview;
import com.kinetiq.overview.domain.model.ClientStatus;
import com.kinetiq.overview.domain.model.StatusReason;
import com.kinetiq.plan.domain.model.DraftedPlan;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.readiness.domain.model.Readiness;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * The overview of a trainer's clients. The tiles count the active clients' current weeks; {@code averageRpe} is left
 * out without a set this week, and {@code averageRpeChange} when either week has none. {@code alerts} are most severe
 * first, {@code clients} in name order.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record OverviewResponse(
        @NotNull int activeClients,
        @NotNull int totalClients,
        @NotNull int sessionsDone,
        @NotNull int sessionsPlanned,
        BigDecimal averageRpe,
        BigDecimal averageRpeChange,
        @NotNull int draftsWaiting,
        @NotNull int planChangesWaiting,
        @NotNull List<OverviewAlertResponse> alerts,
        @NotNull List<OverviewClientResponse> clients) {

    /**
     * One thing that asks for the trainer. The plan change fields come with an open plan change, {@code draft} with a
     * draft, and {@code daysWithoutLog}, {@code since} and, once anything was logged, {@code lastSessionName} with
     * no log; everything else is left out.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OverviewAlertResponse(
            @NotNull Overview.Kind kind,
            @NotNull UUID clientId,
            UUID planChangeId,
            String planChangeTitle,
            Overview.Waiting waitingFor,
            DraftedPlan.Kind draft,
            Integer daysWithoutLog,
            LocalDate since,
            String lastSessionName) {}

    /**
     * One client. {@code planWeeks} and {@code planEndsOn} come with a published plan, {@code planWeek} only while it
     * runs; the sessions cover the last 28 days and come with a due session, {@code averageRpe} the last 14 with a set;
     * {@code lastLoggedAt} is left out before the first set, and the readiness without a check-in today.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OverviewClientResponse(
            @NotNull UUID id,
            @NotNull String name,
            Goal goal,
            @NotNull PlanState planState,
            Integer planWeek,
            Integer planWeeks,
            LocalDate planEndsOn,
            @NotNull ClientStatus status,
            @NotNull List<StatusReason> reasons,
            Integer sessionsDone,
            Integer sessionsDue,
            BigDecimal averageRpe,
            Instant lastLoggedAt,
            Integer readiness,
            Readiness.Band readinessBand,
            @NotNull boolean lighterTargets) {}
}
