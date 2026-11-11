package com.kinetiq.planchange.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.infrastructure.input.PlanChangeResponse.PlanChangeExerciseResponse;
import com.kinetiq.planchange.infrastructure.input.PlanChangeResponse.PlanChangeFigureResponse;
import com.kinetiq.planchange.infrastructure.input.PlanChangeResponse.PlanChangeMoveResponse;
import com.kinetiq.planchange.infrastructure.input.PlanChangeResponse.PlanChangeRemovedSessionResponse;
import com.kinetiq.planchange.infrastructure.input.PlanChangeResponse.PlanChangeTargetResponse;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * A plan change of the client, as the client reads it: {@link PlanChangeResponse} with the trainer's name in place of
 * the client's. {@code title}, {@code reason} and the exercise names are in the client's language.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ClientPlanChangeDetailResponse(
        @NotNull UUID id,
        @NotNull String counterpartName,
        @NotNull Rule rule,
        @NotNull PlanChangeState state,
        @NotNull String title,
        @NotNull String reason,
        @NotNull LocalDate effectiveWeek,
        @NotNull LocalDate windowStart,
        @NotNull LocalDate windowEnd,
        @NotNull List<PlanChangeTargetResponse> targets,
        PlanChangeRemovedSessionResponse removedSession,
        @NotNull List<PlanChangeMoveResponse> moves,
        @NotNull List<PlanChangeExerciseResponse> concernedExercises,
        @NotNull List<PlanChangeFigureResponse> figures,
        @NotNull boolean acceptedByTrainer,
        @NotNull boolean acceptedByClient,
        Side declinedBy,
        DeclineReason declineReason,
        String declineNote,
        Instant endedAt) {}
