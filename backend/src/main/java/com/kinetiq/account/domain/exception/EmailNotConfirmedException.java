package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A trainer gave the right password but has not confirmed their email yet. */
public class EmailNotConfirmedException extends DomainException {

    public EmailNotConfirmedException() {
        super("trainer email is not confirmed");
    }

    @Override
    public String code() {
        return "account.email-not-confirmed";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_ALLOWED;
    }
}
