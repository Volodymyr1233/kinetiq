package com.kinetiq.planchange.infrastructure.input;

import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** The open plan change of a client, as the client page shows it; the text is in the trainer's language. */
record ClientPlanChangeResponse(
        @NotNull UUID id,
        @NotNull Rule rule,
        @NotNull PlanChangeState state,
        @NotNull String title,
        @NotNull String reason,
        @NotNull LocalDate effectiveWeek,
        @NotNull boolean acceptedByTrainer,
        @NotNull boolean acceptedByClient) {}
