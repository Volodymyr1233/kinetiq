package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** One answer for an unknown invite, another trainer's, and one that is used, revoked or expired. */
public class InviteNotFoundException extends DomainException {

    public InviteNotFoundException() {
        super("invite is unknown, another trainer's, or no longer open");
    }

    @Override
    public String code() {
        return "invite.not-found";
    }
}
