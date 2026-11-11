package com.kinetiq.readiness.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A check-in answer that calls for lighter targets: sleep 1 or 2, soreness 4 or 5, stress 4 or 5. Declared in the order
 * a reason names them.
 */
public enum Cause {
    SLEEP,
    SORENESS,
    STRESS;

    private static final int POOR_SLEEP_UP_TO = 2;
    private static final int HIGH_FROM = 4;

    /** The causes among the answers, in declaration order; never empty for a check-in in the Recovery day band. */
    public static List<Cause> of(Answers answers) {
        List<Cause> causes = new ArrayList<>();
        if (answers.sleep() <= POOR_SLEEP_UP_TO) {
            causes.add(SLEEP);
        }
        if (answers.soreness() >= HIGH_FROM) {
            causes.add(SORENESS);
        }
        if (answers.stress() >= HIGH_FROM) {
            causes.add(STRESS);
        }
        return List.copyOf(causes);
    }
}
