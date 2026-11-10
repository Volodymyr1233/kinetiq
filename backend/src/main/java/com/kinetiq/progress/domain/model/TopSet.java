package com.kinetiq.progress.domain.model;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * The best set of an exercise in a session or a week. {@code loadKg} is null for a set without a load; {@code amount}
 * is repetitions or seconds, by the exercise's measure.
 */
public record TopSet(BigDecimal loadKg, int amount, int rpe) {

    private static final int MIN_RPE = 5;
    private static final int MAX_RPE = 10;

    // a set without a load sorts below every set with one
    private static final Comparator<TopSet> ORDER = Comparator.comparing(
                    TopSet::loadKg, Comparator.nullsFirst(Comparator.<BigDecimal>naturalOrder()))
            .thenComparingInt(TopSet::amount);

    public TopSet {
        if (amount < 1) {
            throw new IllegalArgumentException("a set has at least one repetition or second");
        }
        if (rpe < MIN_RPE || rpe > MAX_RPE) {
            throw new IllegalArgumentException("rpe out of range");
        }
    }

    /**
     * Whether this set beats {@code other}: a load beats no load, then the higher load, then the higher amount. An
     * equal set is not better, so the earlier one stays the top set.
     */
    public boolean isBetterThan(TopSet other) {
        return ORDER.compare(this, other) > 0;
    }
}
