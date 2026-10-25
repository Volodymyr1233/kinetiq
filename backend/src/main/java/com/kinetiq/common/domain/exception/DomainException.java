package com.kinetiq.common.domain.exception;

import java.util.Map;

/** A broken business rule. The message is for logs only; the client sees only the code. */
public abstract class DomainException extends RuntimeException {

    /** What kind of refusal this is, which decides the response status. */
    public enum Kind {
        INVALID,
        UNAUTHENTICATED,
        NOT_ALLOWED,
        TOO_MANY_ATTEMPTS
    }

    protected DomainException(String message) {
        super(message);
    }

    public abstract String code();

    public Kind kind() {
        return Kind.INVALID;
    }

    /** Extra values the client may show, such as how long to wait. Never personal data. */
    public Map<String, Object> properties() {
        return Map.of();
    }
}
