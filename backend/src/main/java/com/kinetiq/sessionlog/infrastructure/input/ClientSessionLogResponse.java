package com.kinetiq.sessionlog.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.readiness.domain.model.Cause;
import com.kinetiq.sessionlog.domain.model.SessionState;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * One session log of a client as their trainer reads it: the session as the lists show it, and each planned set with
 * its target and, once logged, its log. {@code averageRpe} is left out while no set is logged, {@code finishedAt} and
 * {@code minutes} until the session is finished, and {@code note}, what the client wrote for their trainer, when there
 * is none. The log locks 24 hours after {@code startedAt}. {@code adjustment} is left out unless the day was a recovery
 * day with lighter targets; while they are in use, each changed entry has its {@code planned} target and the sets not
 * logged yet have the lighter ones.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ClientSessionLogResponse(
        @NotNull UUID sessionLogId,
        @NotNull LocalDate date,
        @NotNull DayOfWeek day,
        @NotNull String name,
        @NotNull SessionState state,
        @NotNull boolean locked,
        @NotNull int setsLogged,
        @NotNull int setsPlanned,
        BigDecimal averageRpe,
        @NotNull Instant startedAt,
        Instant finishedAt,
        Integer minutes,
        String note,
        @NotNull List<SessionLogResponse.LoggedEntryResponse> entries,
        LogAdjustmentResponse adjustment) {

    /** The answers that made the day's targets lighter, and whether the client trained with them. */
    record LogAdjustmentResponse(@NotNull List<Cause> causes, @NotNull boolean inUse) {}
}
