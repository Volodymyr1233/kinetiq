package com.kinetiq.sessionlog.domain.model;

import com.kinetiq.sessionlog.domain.exception.SetValuesInvalidException;
import java.math.BigDecimal;

/**
 * What a client did in a set. {@code loadKg} is {@code null} for no added weight; a load of zero is not a load.
 */
public record SetValues(BigDecimal loadKg, int amount, int rpe) {

    public static final int MIN_AMOUNT = 1;
    public static final int MAX_AMOUNT = 3600;
    public static final int MIN_RPE = 5;
    public static final int MAX_RPE = 10;
    public static final String MAX_LOAD_KG = "999.9";
    private static final int LOAD_DECIMALS = 1;

    public SetValues {
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw new SetValuesInvalidException("amount");
        }
        if (rpe < MIN_RPE || rpe > MAX_RPE) {
            throw new SetValuesInvalidException("rpe");
        }
        if (loadKg != null) {
            if (invalidLoad(loadKg)) {
                throw new SetValuesInvalidException("loadKg");
            }
            loadKg = loadKg.setScale(LOAD_DECIMALS);
        }
    }

    private static boolean invalidLoad(BigDecimal load) {
        return load.signum() <= 0
                || load.compareTo(new BigDecimal(MAX_LOAD_KG)) > 0
                || load.stripTrailingZeros().scale() > LOAD_DECIMALS;
    }
}
