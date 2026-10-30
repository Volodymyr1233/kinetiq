package com.kinetiq.quiz.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The client has submitted the quiz before; a submission never changes. */
public class QuizAlreadySubmittedException extends DomainException {

    public QuizAlreadySubmittedException() {
        super("the client has submitted the quiz before");
    }

    @Override
    public String code() {
        return "quiz.already-submitted";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }
}
