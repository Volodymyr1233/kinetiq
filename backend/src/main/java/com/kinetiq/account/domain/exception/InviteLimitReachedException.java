package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The trainer made too many invite requests in the last hour; the limit also slows down probing for accounts. */
public class InviteLimitReachedException extends DomainException {

    public InviteLimitReachedException() {
        super("too many invite requests from this trainer");
    }

    @Override
    public String code() {
        return "invite.limit-reached";
    }

    @Override
    public Kind kind() {
        return Kind.TOO_MANY_ATTEMPTS;
    }
}
