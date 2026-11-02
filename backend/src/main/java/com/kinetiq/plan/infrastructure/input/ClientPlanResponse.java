package com.kinetiq.plan.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * A client's published plan for one plan week. {@code week} is left out once the plan has finished; the sessions
 * repeat every week, in week order.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ClientPlanResponse(
        @NotNull Goal goal,
        @NotNull int weeks,
        @NotNull int currentWeek,
        @NotNull boolean finished,
        @NotNull boolean seen,
        WeekResponse week,
        @NotNull List<PlanResponse.SessionResponse> sessions) {

    /** A plan week: its number and the date of its Monday. */
    record WeekResponse(@NotNull int number, @NotNull LocalDate startsOn) {}
}
