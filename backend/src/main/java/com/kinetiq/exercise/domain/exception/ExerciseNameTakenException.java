package com.kinetiq.exercise.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** Another exercise the trainer sees already has the name, ignoring letter case. */
public class ExerciseNameTakenException extends DomainException {

    public ExerciseNameTakenException() {
        super("an exercise the trainer sees has this name");
    }

    @Override
    public String code() {
        return "exercise.name-taken";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
