package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** One answer for an unknown email and a wrong password, so neither reveals whether an account exists. */
public class CredentialsInvalidException extends DomainException {

    public CredentialsInvalidException() {
        super("email or password is wrong");
    }

    @Override
    public String code() {
        return "account.credentials-invalid";
    }

    @Override
    public Kind kind() {
        return Kind.UNAUTHENTICATED;
    }
}
