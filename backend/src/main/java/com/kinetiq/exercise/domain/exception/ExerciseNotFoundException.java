package com.kinetiq.exercise.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** One answer for an unknown exercise, another trainer's, and a starter exercise. */
public class ExerciseNotFoundException extends DomainException {

    public ExerciseNotFoundException() {
        super("exercise is unknown, another trainer's, or a starter exercise");
    }

    @Override
    public String code() {
        return "exercise.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
