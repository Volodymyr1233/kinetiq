package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The account of a sign-in no longer exists, so the person counts as signed out. */
public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException() {
        super("account of the sign-in no longer exists");
    }

    @Override
    public String code() {
        return "request.unauthenticated";
    }

    @Override
    public Kind kind() {
        return Kind.UNAUTHENTICATED;
    }
}
