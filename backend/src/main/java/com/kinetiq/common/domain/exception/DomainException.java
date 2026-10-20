package com.kinetiq.common.domain.exception;

/** A broken business rule. The message is for logs only; the client sees only the code. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public abstract String code();
}
