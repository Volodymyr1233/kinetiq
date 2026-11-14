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
 * How a finished session went. {@code volumeKg} is left out when no logged set counts towards it: sets measured in
 * seconds, and sets without a load. {@code startedAt} tells when the 24 hours of changes end. {@code note} is left out when the client wrote none.
 * {@code prs} are the session's PRs in the order of its exercises, and {@code weeksInARow} is the client's week streak.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SessionSummaryResponse(
        @NotNull UUID sessionLogId,
        @NotNull String name,
        @NotNull LocalDate date,
        @NotNull Instant startedAt,
        @NotNull int setsLogged,
        @NotNull int setsPlanned,
        BigDecimal volumeKg,
        @NotNull BigDecimal averageRpe,
        @NotNull int minutes,
        String note,
        @NotNull List<SummaryPrResponse> prs,
        @NotNull int weeksInARow) {

    /**
     * One PR of the session: its exercise, named in the request's language, and its top set. {@code loadKg} is left out
     * for a set without a load.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SummaryPrResponse(
            @NotNull UUID exerciseId,
            @NotNull String name,
            @NotNull Measure measure,
            BigDecimal loadKg,
            @NotNull int amount) {}
}
