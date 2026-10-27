package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/**
 * One answer for an expired, used, revoked, replaced or unknown invite link, and one whose email already has an
 * account, so none of them can be told apart.
 */
public class InviteLinkInvalidException extends DomainException {

    public InviteLinkInvalidException() {
        super("invite link is expired, used, revoked, replaced, unknown or its email has an account");
    }

    @Override
    public String code() {
        return "invite.link-invalid";
    }
}
