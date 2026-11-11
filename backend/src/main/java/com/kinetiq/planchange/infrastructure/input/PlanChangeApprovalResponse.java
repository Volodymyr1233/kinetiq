package com.kinetiq.planchange.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Side;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * A plan change as a row of the trainer's lists. {@code title} and {@code reason} are in the trainer's language; the
 * decline fields are given only once a person kept the current plan.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record PlanChangeApprovalResponse(
        @NotNull UUID id,
        @NotNull UUID clientId,
        @NotNull String clientName,
        @NotNull String title,
        @NotNull String reason,
        @NotNull PlanChangeState state,
        @NotNull boolean acceptedByTrainer,
        @NotNull boolean acceptedByClient,
        Side declinedBy,
        DeclineReason declineReason,
        @NotNull LocalDate effectiveWeek,
        @NotNull Instant createdAt,
        Instant endedAt) {}
