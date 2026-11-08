package com.kinetiq.readiness.domain.model;

/** How ready a client is to train today, from 0 to 100, and the band it falls in. */
public record Readiness(int score, Band band) {

    private static final int READY_FROM = 70;
    private static final int TAKE_IT_EASIER_FROM = 45;
    private static final double MAX_SUM = 12.0;

    public enum Band {
        READY,
        TAKE_IT_EASIER,
        RECOVERY_DAY
    }

    /**
     * The mean of {@code (sleep − 1) / 4}, {@code (5 − soreness) / 4} and {@code (5 − stress) / 4}, times 100 and rounded
     * to a whole number. It equals {@code 100 × sum / 12} of the three parts, which is never halfway between two whole
     * numbers. The band is {@code READY} from 70, {@code TAKE_IT_EASIER} from 45 and {@code RECOVERY_DAY} below.
     */
    public static Readiness of(Answers answers) {
        int sum = (answers.sleep() - Answers.MIN) + (Answers.MAX - answers.soreness()) + (Answers.MAX - answers.stress());
        int score = (int) Math.round(sum * 100 / MAX_SUM);
        return new Readiness(score, band(score));
    }

    private static Band band(int score) {
        if (score >= READY_FROM) {
            return Band.READY;
        }
        return score >= TAKE_IT_EASIER_FROM ? Band.TAKE_IT_EASIER : Band.RECOVERY_DAY;
    }
}
