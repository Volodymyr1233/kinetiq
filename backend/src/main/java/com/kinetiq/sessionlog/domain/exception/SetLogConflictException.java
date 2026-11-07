package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A set id or a place of the session is already used by another set log. */
public class SetLogConflictException extends DomainException {

    private final String code;

    private SetLogConflictException(String code) {
        super("set log conflict: " + code);
        this.code = code;
    }

    /** The id already logs another place of the session. */
    public static SetLogConflictException idInUse() {
        return new SetLogConflictException("set-log.id-in-use");
    }

    /** The place is already logged under another id. */
    public static SetLogConflictException placeTaken() {
        return new SetLogConflictException("set-log.place-taken");
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
