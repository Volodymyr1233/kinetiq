package com.kinetiq.account.domain.model;

/** Expired is not a status: it follows from the expiry time. */
public enum InviteStatus {
    OPEN,
    USED,
    REVOKED
}
