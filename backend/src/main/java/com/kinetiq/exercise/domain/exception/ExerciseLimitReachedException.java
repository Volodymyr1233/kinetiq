package com.kinetiq.exercise.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The trainer already owns as many exercises as anyone may. */
public class ExerciseLimitReachedException extends DomainException {

    public ExerciseLimitReachedException() {
        super("trainer owns the maximum number of exercises");
    }

    @Override
    public String code() {
        return "exercise.limit-reached";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
