package com.kinetiq.readiness.domain.model;

import com.kinetiq.common.domain.exception.InvalidValueException;

/** A client's answers to the check-in: how they slept, how sore and how stressed they are, each from 1 to 5. */
public record Answers(int sleep, int soreness, int stress) {

    public static final int MIN = 1;
    public static final int MAX = 5;

    public Answers {
        require("sleep", sleep);
        require("soreness", soreness);
        require("stress", stress);
    }

    private static void require(String answer, int value) {
        if (value < MIN || value > MAX) {
            throw new InvalidValueException(answer + " is outside 1 to 5");
        }
    }
}
