package com.kinetiq.planchange.domain.model;

/** A condition over a client's logs that calls for a plan change. Declared in priority order. */
public enum Rule {
    LOW_COMPLETION(2),
    HIGH_RPE(2),
    STALLED_WEIGHT(3);

    private final int weeks;

    Rule(int weeks) {
        this.weeks = weeks;
    }

    /** How many completed weeks the rule looks at, and how long it stays quiet after a plan change ends. */
    public int weeks() {
        return weeks;
    }
}
