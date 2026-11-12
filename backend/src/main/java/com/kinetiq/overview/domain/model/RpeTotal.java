package com.kinetiq.overview.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** The sum and the number of the RPE of some logged sets, so the sets of several days or clients add up. */
public record RpeTotal(int sum, int sets) {

    public static final RpeTotal NONE = new RpeTotal(0, 0);

    public RpeTotal {
        if (sum < 0 || sets < 0) {
            throw new IllegalArgumentException("a sum of sets is never negative");
        }
    }

    public RpeTotal plus(RpeTotal other) {
        return new RpeTotal(sum + other.sum, sets + other.sets);
    }

    /** The mean RPE to one decimal, half up; empty without sets. */
    public Optional<BigDecimal> mean() {
        if (sets == 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(sets), 1, RoundingMode.HALF_UP));
    }
}
