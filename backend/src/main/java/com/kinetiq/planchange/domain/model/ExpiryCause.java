package com.kinetiq.planchange.domain.model;

/** Why a plan change expired: nobody decided in time, or the plan no longer fits the content. */
public enum ExpiryCause {
    /** Trainer and client had not both accepted it when its effective week began. */
    UNDECIDED,
    /** Both accepted it, but the plan was edited so that the content no longer fits, or the plan is gone. */
    PLAN_EDITED
}
