package com.kinetiq.readiness.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * What a set asks for: load, repetitions or seconds, and RPE. {@code loadKg} is {@code null} when there is no added
 * weight, and otherwise kept to one decimal like every load.
 */
public record Target(BigDecimal loadKg, int amount, int rpe) {

    public Target {
        if (loadKg != null) {
            loadKg = loadKg.setScale(1, RoundingMode.HALF_UP);
        }
    }
}
