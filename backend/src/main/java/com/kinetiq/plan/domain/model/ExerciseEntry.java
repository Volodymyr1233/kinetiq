package com.kinetiq.plan.domain.model;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.PlanRuleViolationException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One exercise in its place in a session, with its targets. {@code amount} counts repetitions or seconds, as the
 * exercise measures; {@code loadKg} is {@code null} when there is no added weight.
 */
public record ExerciseEntry(ExerciseId exercise, int sets, int amount, BigDecimal loadKg, int rpe, int restSeconds) {

    public static final int MIN_SETS = 1;
    public static final int MAX_SETS = Exercise.MAX_SETS;
    public static final int MIN_AMOUNT = 1;
    public static final int MAX_AMOUNT = Exercise.MAX_AMOUNT;
    public static final String MAX_LOAD_KG = "999.9";
    public static final int LOAD_DECIMALS = 1;
    public static final int MIN_RPE = 5;
    public static final int MAX_RPE = 10;
    public static final int MIN_REST_SECONDS = 0;
    public static final int MAX_REST_SECONDS = Exercise.MAX_REST_SECONDS;

    public ExerciseEntry {
        Objects.requireNonNull(exercise);
        if (outside(sets, MIN_SETS, MAX_SETS)) {
            throw new PlanRuleViolationException("plan.sets-out-of-range");
        }
        if (outside(amount, MIN_AMOUNT, MAX_AMOUNT)) {
            throw new PlanRuleViolationException("plan.amount-out-of-range");
        }
        if (loadKg != null) {
            if (invalidLoad(loadKg)) {
                throw new PlanRuleViolationException("plan.load-out-of-range");
            }
            loadKg = loadKg.setScale(LOAD_DECIMALS);
        }
        if (outside(rpe, MIN_RPE, MAX_RPE)) {
            throw new PlanRuleViolationException("plan.rpe-out-of-range");
        }
        if (outside(restSeconds, MIN_REST_SECONDS, MAX_REST_SECONDS)) {
            throw new PlanRuleViolationException("plan.rest-out-of-range");
        }
    }

    private static boolean invalidLoad(BigDecimal load) {
        return load.signum() <= 0
                || load.compareTo(new BigDecimal(MAX_LOAD_KG)) > 0
                || load.stripTrailingZeros().scale() > LOAD_DECIMALS;
    }

    private static boolean outside(int value, int min, int max) {
        return value < min || value > max;
    }
}
