package com.kinetiq.planchange.domain.model;

/** A value of an exercise entry that a plan change can change. {@code AMOUNT} counts repetitions or seconds. */
public enum Target {
    SETS,
    AMOUNT,
    LOAD_KG,
    RPE,
    REST_SECONDS
}
