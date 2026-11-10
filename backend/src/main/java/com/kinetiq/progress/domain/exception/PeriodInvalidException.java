package com.kinetiq.progress.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** A period of progress that is not 4, 8 or 12 weeks. */
public class PeriodInvalidException extends DomainException {

    public PeriodInvalidException() {
        super("progress period invalid");
    }

    @Override
    public String code() {
        return "progress.period-invalid";
    }

    @Override
    public Kind kind() {
        return Kind.INVALID;
    }
}
