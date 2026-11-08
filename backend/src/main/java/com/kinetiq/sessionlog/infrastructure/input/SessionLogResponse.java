package com.kinetiq.sessionlog.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.exercise.domain.model.Measure;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * A session a client started: a copy of the planned session, with each planned set and the set logged for it.
 * {@code finishedAt} is left out until the session is finished.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SessionLogResponse(
        @NotNull UUID id,
        @NotNull LocalDate date,
        @NotNull String name,
        @NotNull Instant startedAt,
        Instant finishedAt,
        @NotNull List<LoggedEntryResponse> entries) {

    /** One exercise of the session, with its name in the language of the request. */
    record LoggedEntryResponse(
            @NotNull int position,
            @NotNull UUID exerciseId,
            @NotNull String exerciseName,
            @NotNull Measure measure,
            @NotNull int restSeconds,
            @NotNull List<PlannedSetResponse> sets) {}

    /** A planned set; {@code log} is left out until the set is logged. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PlannedSetResponse(@NotNull int number, @NotNull TargetResponse target, LoggedSetResponse log) {}

    /** What the plan asked for in a set; {@code loadKg} is left out when there is no added weight. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TargetResponse(BigDecimal loadKg, @NotNull int amount, @NotNull int rpe) {}

    /** What the client did in a set. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record LoggedSetResponse(
            @NotNull UUID id, BigDecimal loadKg, @NotNull int amount, @NotNull int rpe, @NotNull Instant loggedAt) {}
}
