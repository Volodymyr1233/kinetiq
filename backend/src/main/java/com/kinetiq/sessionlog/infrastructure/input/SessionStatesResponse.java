package com.kinetiq.sessionlog.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.sessionlog.domain.model.SessionState;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** Where each planned session of a plan week stands, in week order; {@code today} is the client's local date. */
record SessionStatesResponse(@NotNull LocalDate today, @NotNull List<SessionStateResponse> sessions) {

    /**
     * One planned session. {@code state} is left out before the plan was published, and {@code sessionLogId} and
     * {@code startedAt} when the session was never started. {@code adjusted} says a recovery day's lighter targets are
     * in use for it.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SessionStateResponse(
            @NotNull DayOfWeek day,
            @NotNull LocalDate date,
            SessionState state,
            UUID sessionLogId,
            Instant startedAt,
            @NotNull boolean adjusted) {}
}
