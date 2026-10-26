package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A client tried to change their name, which only a trainer may do. */
public class NameNotChangeableException extends DomainException {

    public NameNotChangeableException() {
        super("only a trainer changes their name");
    }

    @Override
    public String code() {
        return "account.name-not-changeable";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_ALLOWED;
    }
}
