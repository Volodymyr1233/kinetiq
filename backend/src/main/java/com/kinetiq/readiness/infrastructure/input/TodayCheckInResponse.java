package com.kinetiq.readiness.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.readiness.domain.model.Readiness;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * The client's check-in today, their local date. {@code open} tells whether one can still be saved; {@code checkIn} is
 * left out until the client checks in.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TodayCheckInResponse(@NotNull LocalDate date, @NotNull boolean open, CheckInResponse checkIn) {

    /** The answers and the readiness the server derived from them. */
    record CheckInResponse(
            @NotNull int sleep,
            @NotNull int soreness,
            @NotNull int stress,
            @NotNull int score,
            @NotNull Readiness.Band band,
            @NotNull Instant savedAt) {}
}
