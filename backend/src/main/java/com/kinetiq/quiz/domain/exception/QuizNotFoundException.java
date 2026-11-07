package com.kinetiq.quiz.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;

/** The client has no stored answers, for instance because they have not submitted the quiz. */
public class QuizNotFoundException extends DomainException {

    public QuizNotFoundException() {
        super("the client has no quiz answers");
    }

    @Override
    public String code() {
        return "quiz.not-found";
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
