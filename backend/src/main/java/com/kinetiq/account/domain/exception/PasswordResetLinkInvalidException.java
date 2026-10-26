package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** One answer for a used, expired, replaced or unknown link, so none of them can be told apart. */
public class PasswordResetLinkInvalidException extends DomainException {

    public PasswordResetLinkInvalidException() {
        super("password reset link is used, expired, replaced or unknown");
    }

    @Override
    public String code() {
        return "account.password-reset-link-invalid";
    }
}
