package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The quiz comes first: no plan can be built for a client who has not submitted it. */
public class PlanQuizNotSubmittedException extends DomainException {

    public PlanQuizNotSubmittedException() {
        super("client has not submitted the quiz");
    }

    @Override
    public String code() {
        return "plan.quiz-not-submitted";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
