package com.kinetiq.readiness.domain.model;

/**
 * A check-in answer that calls for lighter targets: sleep 1 or 2, soreness 4 or 5, stress 4 or 5. Declared in the order
 * a reason names them.
 */
public enum Cause {
    SLEEP,
    SORENESS,
    STRESS
}
