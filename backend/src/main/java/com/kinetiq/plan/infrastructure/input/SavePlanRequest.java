package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

/** A plan as a whole. {@code version} is the one this editor read, and left out on a first save. */
record SavePlanRequest(
        Long version,
        @NotNull Goal goal,
        @NotNull @Min(Plan.MIN_WEEKS) @Max(Plan.MAX_WEEKS) Integer weeks,
        @NotNull @Size(max = Plan.MAX_SESSIONS) List<@NotNull @Valid SessionRequest> sessions) {

    record SessionRequest(
            @NotBlank @Size(max = Session.NAME_MAX_LENGTH) String name,
            @NotNull DayOfWeek day,
            @NotNull @Size(max = Session.MAX_EXERCISES) List<@NotNull @Valid EntryRequest> entries) {}

    /** One exercise with its targets; {@code loadKg} is left out when there is no added weight. */
    record EntryRequest(
            @NotNull UUID exerciseId,
            @NotNull @Min(ExerciseEntry.MIN_SETS) @Max(ExerciseEntry.MAX_SETS) Integer sets,
            @NotNull @Min(ExerciseEntry.MIN_AMOUNT) @Max(ExerciseEntry.MAX_AMOUNT) Integer amount,
            @DecimalMin("0.1") @Digits(integer = 3, fraction = ExerciseEntry.LOAD_DECIMALS) BigDecimal loadKg,
            @NotNull @Min(ExerciseEntry.MIN_RPE) @Max(ExerciseEntry.MAX_RPE) Integer rpe,
            @NotNull @Min(ExerciseEntry.MIN_REST_SECONDS) @Max(ExerciseEntry.MAX_REST_SECONDS) Integer restSeconds) {}
}
