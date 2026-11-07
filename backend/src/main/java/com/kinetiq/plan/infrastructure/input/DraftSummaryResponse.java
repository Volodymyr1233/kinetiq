package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.quiz.domain.model.Goal;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.util.List;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** What a client may read of their own draft, with the training days in week order. */
record DraftSummaryResponse(@NotNull List<DayOfWeek> trainingDays, @NotNull int weeks, @NotNull Goal goal) {}
