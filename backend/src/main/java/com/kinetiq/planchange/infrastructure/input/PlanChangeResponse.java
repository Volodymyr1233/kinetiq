package com.kinetiq.planchange.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.model.Target;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * A plan change of the trainer's own client. {@code title} and {@code reason} are in the trainer's language; targets are
 * ordered by weekday, position and target. {@code removedSession} is left out unless a session goes. The decision fields
 * say who accepted and, once the plan change is declined, who kept the current plan and why.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record PlanChangeResponse(
        @NotNull UUID id,
        @NotNull UUID clientId,
        @NotNull String clientName,
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
        Instant endedAt) {

    /** One changed value of an entry, with the exercise name in the trainer's language and what its amount counts. */
    record PlanChangeTargetResponse(
            @NotNull DayOfWeek sessionDay,
            @NotNull String sessionName,
            @NotNull int position,
            @NotNull UUID exerciseId,
            @NotNull String exerciseName,
            @NotNull Measure measure,
            @NotNull Target target,
            @NotNull BigDecimal oldValue,
            @NotNull BigDecimal newValue) {}

    record PlanChangeRemovedSessionResponse(@NotNull DayOfWeek day, @NotNull String name) {}

    record PlanChangeMoveResponse(
            @NotNull UUID exerciseId, @NotNull String exerciseName, @NotNull DayOfWeek fromDay, @NotNull DayOfWeek toDay) {}

    record PlanChangeExerciseResponse(@NotNull UUID id, @NotNull String name) {}

    /** A number the rule compared; {@code exerciseId} is given only for a best load. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PlanChangeFigureResponse(
            @NotNull FigureKind kind, UUID exerciseId, @NotNull LocalDate weekStart, @NotNull BigDecimal value) {}
}
