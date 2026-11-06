package com.kinetiq.plan.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.model.AutomaticDraft;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * A client's plan, sessions in week order. {@code firstWeek} and {@code currentWeek} are left out for a draft;
 * {@code currentWeek} is at least 1, and above {@code weeks} once the plan has finished. {@code automaticDraft} is left
 * out unless the system built this draft, or failed to, and the trainer has not saved it yet.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record PlanResponse(
        @NotNull Status status,
        @NotNull Goal goal,
        @NotNull int weeks,
        LocalDate firstWeek,
        Integer currentWeek,
        @NotNull long version,
        AutomaticDraft automaticDraft,
        @NotNull List<SessionResponse> sessions) {

    enum Status {
        DRAFT,
        PUBLISHED
    }

    record SessionResponse(@NotNull String name, @NotNull DayOfWeek day, @NotNull List<EntryResponse> entries) {}

    /** An exercise's targets, with its name in the language of the request; {@code loadKg} is left out when empty. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record EntryResponse(
            @NotNull UUID exerciseId,
            @NotNull String exerciseName,
            @NotNull Measure measure,
            @NotNull int sets,
            @NotNull int amount,
            BigDecimal loadKg,
            @NotNull int rpe,
            @NotNull int restSeconds) {}
}
