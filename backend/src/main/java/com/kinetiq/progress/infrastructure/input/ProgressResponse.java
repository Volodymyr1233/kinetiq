package com.kinetiq.progress.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * A client's progress over one period. {@code week} is the current plan week, the last one once the plan has finished,
 * and {@code weeks} the period's plan weeks, oldest first. {@code averageRpe} is left out without a set in the period,
 * and each change, the last completed week against the week before it, when either has no value for it.
 * {@code exercises} are those logged in the period, the one in the most sessions first, then the most recently logged.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ProgressResponse(
        @NotNull Goal goal,
        @NotNull int week,
        @NotNull boolean finished,
        @NotNull int sessionsDone,
        @NotNull int sessionsPlanned,
        Integer sessionsDoneChange,
        BigDecimal averageRpe,
        BigDecimal averageRpeChange,
        @NotNull int weeksInARow,
        @NotNull List<ProgressWeekResponse> weeks,
        @NotNull List<ExerciseProgressResponse> exercises) {

    /** One plan week; {@code running} marks the current week of a plan that has not finished. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ProgressWeekResponse(
            @NotNull int number,
            @NotNull LocalDate startsOn,
            @NotNull int planned,
            @NotNull int done,
            @NotNull boolean running,
            BigDecimal averageRpe) {}

    /**
     * One exercise logged in the period, named in the request's language: its top set in the period, the weeks it was
     * logged in and its 3 newest sessions over all logs, newest first. {@code topSetChange} is left out when it cannot
     * be compared.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ExerciseProgressResponse(
            @NotNull UUID exerciseId,
            @NotNull String name,
            @NotNull Measure measure,
            @NotNull TopSetResponse best,
            TopSetChangeResponse topSetChange,
            @NotNull boolean prToday,
            @NotNull List<ExerciseWeekResponse> weeks,
            @NotNull List<RecentSetResponse> recent) {}

    /** The exercise's top set and mean RPE in one plan week. */
    record ExerciseWeekResponse(@NotNull int number, @NotNull TopSetResponse top, @NotNull BigDecimal averageRpe) {}

    /** The exercise's top set in one session log. */
    record RecentSetResponse(@NotNull LocalDate date, @NotNull TopSetResponse top) {}

    /** A top set; {@code loadKg} is left out for a set without a load, and {@code amount} is repetitions or seconds. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TopSetResponse(BigDecimal loadKg, @NotNull int amount, @NotNull int rpe) {}

    /** A change of a top set: exactly one of {@code loadKg} and {@code amount} is present. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TopSetChangeResponse(BigDecimal loadKg, Integer amount) {}
}
