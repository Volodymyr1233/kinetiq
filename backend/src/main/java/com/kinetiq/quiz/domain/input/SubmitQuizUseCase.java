package com.kinetiq.quiz.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.QuizStatus;

public interface SubmitQuizUseCase {

    /**
     * Stores the client's answers once, with the time of submitting, and returns the client's status after it. Refused
     * when the client has submitted before, also when two submits arrive at the same moment.
     */
    QuizStatus submit(SubmitQuizCommand command);

    /** Tells whether the client has submitted and whether a plan is published. */
    QuizStatus status(AccountId client);
}
