package com.kinetiq.sessionlog.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * How a finished session went. {@code volumeKg} is left out when no logged set counts towards it: sets measured in
 * seconds, and sets without a load. {@code startedAt} tells when the 24 hours of changes end. {@code note} is left out when the client wrote none.
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
        String note) {}
