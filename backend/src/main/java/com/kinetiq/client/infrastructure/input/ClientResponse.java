package com.kinetiq.client.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** One client in the trainer's list; {@code goal} is left out until the client submits the quiz. */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ClientResponse(@NotNull UUID id, @NotNull String name, Goal goal, @NotNull PlanState planState) {}
