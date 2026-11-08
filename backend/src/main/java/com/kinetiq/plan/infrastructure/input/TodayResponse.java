package com.kinetiq.plan.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.plan.domain.input.TodayView;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** What a client trains on {@code date}, their local date. {@code session} is left out unless {@code state} is SESSION, {@code next} unless it is REST with a session left. */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TodayResponse(
        @NotNull LocalDate date,
        @NotNull TodayView.State state,
        PlanResponse.SessionResponse session,
        NextResponse next) {

    /** The first session after {@code date} of a rest day, and the date it falls on. */
    record NextResponse(@NotNull LocalDate date, @NotNull PlanResponse.SessionResponse session) {}
}
