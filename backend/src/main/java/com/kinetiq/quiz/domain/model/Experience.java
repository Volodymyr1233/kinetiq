package com.kinetiq.quiz.domain.model;

/** The quiz's step 2, how long the client has trained, codes in screen order (spec 50, data model). */
public enum Experience {
    LESS_THAN_3_MONTHS,
    MONTHS_3_TO_6,
    MONTHS_6_TO_12,
    YEARS_1_TO_2,
    YEARS_2_TO_5,
    YEARS_5_PLUS
}
