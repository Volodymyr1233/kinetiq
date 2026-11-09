package com.kinetiq.readiness.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.readiness.domain.model.Cause;
import com.kinetiq.readiness.domain.model.Readiness;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * The client's check-in today, their local date. {@code open} tells whether one can still be saved and the targets
 * switched; {@code checkIn} is left out until the client checks in.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TodayCheckInResponse(@NotNull LocalDate date, @NotNull boolean open, CheckInResponse checkIn) {

    /** The answers and the readiness the server derived from them; {@code adjustment} is left out unless lighter. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CheckInResponse(
            @NotNull int sleep,
            @NotNull int soreness,
            @NotNull int stress,
            @NotNull int score,
            @NotNull Readiness.Band band,
            @NotNull Instant savedAt,
            AdjustmentResponse adjustment) {}

    /**
     * Today's lighter targets, one for each entry of today's session in plan order, and the answers that caused them.
     * {@code inUse} is false once the client chose the original targets.
     */
    record AdjustmentResponse(
            @NotNull boolean inUse,
            @NotNull List<Cause> causes,
            @NotNull List<AdjustedTargetResponse> entries) {}

    /** The lighter target of one entry; {@code loadKg} is left out when there is no added weight. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AdjustedTargetResponse(@NotNull int position, BigDecimal loadKg, @NotNull int amount, @NotNull int rpe) {}
}
