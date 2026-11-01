package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** One answer for another trainer's exercise and an unknown one, so neither is revealed. */
public class PlanExerciseNotInLibraryException extends DomainException {

    public PlanExerciseNotInLibraryException() {
        super("exercise is not in the trainer's library");
    }

    @Override
    public String code() {
        return "plan.exercise-not-in-library";
    }
}
