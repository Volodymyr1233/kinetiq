package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The invited email already has an account, of any role, confirmed or not. */
public class InviteAccountExistsException extends DomainException {

    public InviteAccountExistsException() {
        super("invited email already has an account");
    }

    @Override
    public String code() {
        return "invite.account-exists";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
