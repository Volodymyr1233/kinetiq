package com.kinetiq.common.domain.model;

/** What a notification tells; some kinds go to a client, some to a trainer and some to either. */
public enum NoticeKind {
    /** A client: the trainer published their plan. */
    PLAN_PUBLISHED,
    /** A client: the trainer saved a change to their published plan. */
    PLAN_CHANGED,
    /** A client or a trainer: a plan change waits for a decision. */
    PLAN_CHANGE_WAITING,
    /** A client or a trainer: the other person decided on a plan change. */
    PLAN_CHANGE_DECISION,
    /** A client or a trainer: the other person wrote a message. */
    NEW_MESSAGE,
    /** A trainer: a client's first plan was drafted. */
    DRAFT_TO_REVIEW,
    /** A trainer: a client's check-in led to a recovery day. */
    RECOVERY_DAY,
    /** A trainer: a client deleted their account. */
    CLIENT_DELETED
}
