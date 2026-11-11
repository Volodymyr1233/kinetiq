package com.kinetiq.readiness.domain.model;

import com.kinetiq.account.domain.model.WeightUnit;
import com.kinetiq.exercise.domain.model.Measure;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Today's lighter targets on a recovery day, one for each planned entry by position, with the answers that caused
 * them. {@code inUse} is false once the client chose the original targets. The plan itself never changes.
 */
public record Adjustment(List<Target> lighter, List<Cause> causes, boolean inUse) {

    private static final BigDecimal KG_PER_LB = new BigDecimal("0.45359237");
    private static final BigDecimal NINE_TENTHS = new BigDecimal("0.9");
    private static final BigDecimal HALF_KG = new BigDecimal("0.5");
    private static final int MIN_RPE = 5;
    private static final int MIN_AMOUNT = 1;

    public Adjustment {
        lighter = List.copyOf(lighter);
        causes = List.copyOf(causes);
    }

    /**
     * The adjustment of a check-in over today's planned entries: empty unless its band is {@code RECOVERY_DAY} and at
     * least one target gets lighter.
     */
    public static Optional<Adjustment> of(Answers answers, boolean inUse, List<Planned> entries, WeightUnit unit) {
        if (Readiness.of(answers).band() != Readiness.Band.RECOVERY_DAY) {
            return Optional.empty();
        }
        List<Target> lighter = entries.stream()
                .map(entry -> lighter(entry.target(), entry.measure(), unit))
                .toList();
        boolean changes = false;
        for (int position = 0; position < entries.size(); position++) {
            changes |= !lighter.get(position).equals(entries.get(position).target());
        }
        return changes ? Optional.of(new Adjustment(lighter, Cause.of(answers), inUse)) : Optional.empty();
    }

    /**
     * One target made lighter. With a load: 10% less, rounded to 0.5 kg or a whole pound in the client's unit with an
     * exact half going to the lighter value, the amount unchanged; the planned load stays when that is not lighter or
     * falls below one step. Without a load: one repetition fewer, or 10% fewer seconds rounded down, never below 1.
     * Target RPE one lower, never below 5.
     */
    public static Target lighter(Target planned, Measure measure, WeightUnit unit) {
        int rpe = Math.max(MIN_RPE, planned.rpe() - 1);
        if (planned.loadKg() != null) {
            return new Target(lighterLoad(planned.loadKg(), unit), planned.amount(), rpe);
        }
        int amount = measure == Measure.REPS
                ? planned.amount() - 1
                : NINE_TENTHS.multiply(BigDecimal.valueOf(planned.amount())).intValue();
        return new Target(null, Math.max(MIN_AMOUNT, amount), rpe);
    }

    /** The target in use at {@code position}: the lighter one while in use, otherwise the planned one. */
    public Target inUse(int position, Target planned) {
        return inUse ? lighter.get(position) : planned;
    }

    private static BigDecimal lighterLoad(BigDecimal kg, WeightUnit unit) {
        BigDecimal lighter;
        BigDecimal step;
        if (unit == WeightUnit.LB) {
            BigDecimal pounds = kg.divide(KG_PER_LB, 10, RoundingMode.HALF_EVEN)
                    .multiply(NINE_TENTHS)
                    .setScale(0, RoundingMode.HALF_DOWN);
            lighter = pounds.multiply(KG_PER_LB).setScale(1, RoundingMode.HALF_UP);
            step = KG_PER_LB;
        } else {
            // a multiple of 0.5 kg: round twice the value to a whole number
            lighter = kg.multiply(NINE_TENTHS)
                    .multiply(BigDecimal.TWO)
                    .setScale(0, RoundingMode.HALF_DOWN)
                    .divide(BigDecimal.TWO, 1, RoundingMode.UNNECESSARY);
            step = HALF_KG;
        }
        return lighter.compareTo(kg) < 0 && lighter.compareTo(step.setScale(1, RoundingMode.HALF_UP)) >= 0
                ? lighter
                : kg;
    }
}
