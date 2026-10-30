package com.kinetiq.readiness.domain.model;

import com.kinetiq.common.domain.exception.InvalidValueException;

/** How prepared a client is to train today, from 0 to 100. */
public record Readiness(int score) {

    public static final int MIN_ANSWER = 1;
    public static final int MAX_ANSWER = 5;

    private static final int READY_FROM = 70;
    private static final int TAKE_IT_EASIER_FROM = 45;

    public Readiness {
        if (score < 0 || score > 100) {
            throw new InvalidValueException("readiness is outside 0 to 100");
        }
    }

    /** Scores a check-in. Answers run from 1 to 5; more sleep is better, more soreness and stress are worse. */
    public static Readiness fromCheckIn(int sleep, int soreness, int stress) {
        requireAnswer(sleep);
        requireAnswer(soreness);
        requireAnswer(stress);
        // each answer maps to 0..4, so the mean of the three is sum / 12 of the way to 100
        int sum = (sleep - MIN_ANSWER) + (MAX_ANSWER - soreness) + (MAX_ANSWER - stress);
        return new Readiness((int) Math.round(sum * 100.0 / 12));
    }

    public ReadinessBand band() {
        if (score >= READY_FROM) {
            return ReadinessBand.READY;
        }
        return score >= TAKE_IT_EASIER_FROM ? ReadinessBand.TAKE_IT_EASIER : ReadinessBand.RECOVERY_DAY;
    }

    private static void requireAnswer(int answer) {
        if (answer < MIN_ANSWER || answer > MAX_ANSWER) {
            throw new InvalidValueException("check-in answer is outside 1 to 5");
        }
    }
}
