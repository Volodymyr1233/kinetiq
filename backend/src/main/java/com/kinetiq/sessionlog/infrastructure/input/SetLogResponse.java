package com.kinetiq.sessionlog.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** A set as logged, with its place in the session; {@code loadKg} is left out when there is no added weight. */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SetLogResponse(
        @NotNull UUID id,
        @NotNull int entryPosition,
        @NotNull int setNumber,
        BigDecimal loadKg,
        @NotNull int amount,
        @NotNull int rpe,
        @NotNull Instant loggedAt) {}
