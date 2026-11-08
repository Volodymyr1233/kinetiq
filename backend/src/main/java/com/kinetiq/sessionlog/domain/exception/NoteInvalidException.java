package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The note is longer than the limit. */
public class NoteInvalidException extends DomainException {

    public NoteInvalidException() {
        super("session note too long");
    }

    @Override
    public String code() {
        return "session-log.note-invalid";
    }
}
