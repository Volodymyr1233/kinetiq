package com.kinetiq.planchange.domain.rule;

/**
 * Why plan change content is refused. Used for the sentences of a second attempt and in tests; never in an API
 * response.
 */
public final class LimitCode {

    public static final String ENTRY_NOT_CONCERNED = "limit.entry-not-concerned";
    public static final String TARGET_NOT_ALLOWED = "limit.target-not-allowed";
    public static final String VOLUME_OUT_OF_RANGE = "limit.volume-out-of-range";
    public static final String LOAD_OUT_OF_RANGE = "limit.load-out-of-range";
    public static final String LOAD_NOT_HALF_KG = "limit.load-not-half-kg";
    public static final String AMOUNT_DROP_TOO_LARGE = "limit.amount-drop-too-large";
    public static final String SESSION_REMOVAL_COUNT = "limit.session-removal-count";
    public static final String TOO_FEW_SESSIONS = "limit.too-few-sessions";
    public static final String TOO_MANY_MOVES = "limit.too-many-moves";
    public static final String MOVE_NOT_FROM_REMOVED = "limit.move-not-from-removed";
    public static final String TOO_MANY_ENTRIES = "limit.too-many-entries";
    public static final String NOTHING_CHANGED = "limit.nothing-changed";
    public static final String PLAN_RULE = "limit.plan-rule";
    public static final String UNKNOWN_KEY = "limit.unknown-key";

    private LimitCode() {}
}
