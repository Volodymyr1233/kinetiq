package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The password a signed-in person typed to confirm an action is not theirs. */
public class PasswordIncorrectException extends DomainException {

    public PasswordIncorrectException() {
        super("password does not match the signed-in account");
    }

    @Override
    public String code() {
        return "account.password-incorrect";
    }
}
